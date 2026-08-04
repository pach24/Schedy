package com.schednd.fakes

import com.schednd.domain.model.Event
import com.schednd.domain.model.Participant
import com.schednd.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/** Sesiones en memoria, con el registro de lo que se le ha pedido para poder afirmarlo. */
class FakeEventRepository(
    events: List<Event> = emptyList(),
    participants: Map<String, List<Participant>> = emptyMap()
) : EventRepository {

    private val events = MutableStateFlow(events.associateBy { it.code })
    private val participants = MutableStateFlow(participants)

    var createdEvents: MutableList<Pair<String, String>> = mutableListOf()
        private set
    var savedAvailability: MutableList<Triple<String, String, List<LocalDate>>> = mutableListOf()
        private set
    var confirmedDates: MutableList<Triple<String, LocalDate, LocalTime?>> = mutableListOf()
        private set
    var deletedCodes: MutableList<String> = mutableListOf()
        private set
    var removedParticipants: MutableList<Pair<String, String>> = mutableListOf()
        private set

    /** Se ejecuta justo antes de servir el listado: sirve para mirar cómo se pidió. */
    var beforeGetEvents: () -> Unit = {}

    /** Si se deja puesto, la próxima lectura del listado falla con esto y se olvida. */
    var failNextGetEvents: Exception? = null

    override suspend fun createEvent(name: String, creatorId: String): String {
        val code = "CODE${createdEvents.size + 1}"
        createdEvents += name to creatorId
        events.value = events.value + (code to Event(code = code, name = name, creatorId = creatorId))
        return code
    }

    override suspend fun getEvent(code: String): Event? = events.value[code]

    override fun observeEvent(code: String): Flow<Event?> = events.map { it[code] }

    override fun observeParticipants(code: String): Flow<List<Participant>> =
        participants.map { it[code].orEmpty() }

    override suspend fun addOrUpdateAvailability(
        code: String,
        userId: String,
        name: String,
        dates: List<LocalDate>,
        notes: List<String>
    ) {
        savedAvailability += Triple(code, userId, dates)
        val current = participants.value[code].orEmpty().filterNot { it.userId == userId }
        participants.value = participants.value + (code to current + Participant(userId, name))
    }

    override suspend fun getEvents(codes: Collection<String>): List<Event> {
        beforeGetEvents()
        failNextGetEvents?.let {
            failNextGetEvents = null
            throw it
        }
        return codes.mapNotNull { events.value[it] }
    }

    override suspend fun removeParticipant(code: String, userId: String) {
        removedParticipants += code to userId
        val rest = participants.value[code].orEmpty().filterNot { it.userId == userId }
        participants.value = participants.value + (code to rest)
    }

    override suspend fun deleteEvent(code: String) {
        deletedCodes += code
        events.value = events.value - code
    }

    override suspend fun doesEventExist(code: String): Boolean = events.value.containsKey(code)

    override suspend fun confirmDate(code: String, date: LocalDate, startTime: LocalTime?) {
        confirmedDates += Triple(code, date, startTime)
    }

    override suspend fun setStartTime(code: String, startTime: LocalTime?) = Unit

    override suspend fun clearConfirmedDate(code: String) {
        confirmedDates.clear()
    }
}
