package com.schednd.domain.repository

import com.schednd.domain.model.Event
import com.schednd.domain.model.Participant
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/** Las sesiones y quién puede cuándo. */
interface EventRepository {

    suspend fun createEvent(name: String, creatorId: String): String

    suspend fun getEvent(code: String): Event?

    fun observeEvent(code: String): Flow<Event?>

    fun observeParticipants(code: String): Flow<List<Participant>>

    suspend fun addOrUpdateAvailability(
        code: String,
        userId: String,
        name: String,
        dates: List<LocalDate>,
        notes: List<String> = emptyList()
    )

    suspend fun getEvents(codes: Collection<String>): List<Event>

    suspend fun deleteEvent(code: String)

    suspend fun doesEventExist(code: String): Boolean

    suspend fun confirmDate(code: String, date: LocalDate, startTime: LocalTime? = null)

    suspend fun setStartTime(code: String, startTime: LocalTime?)

    suspend fun clearConfirmedDate(code: String)
}
