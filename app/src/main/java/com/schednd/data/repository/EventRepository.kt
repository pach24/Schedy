package com.schednd.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.schednd.domain.util.EventCodeGenerator
import com.schednd.domain.model.Event
import com.schednd.domain.model.Participant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val eventsCollection = firestore.collection("events")

    suspend fun createEvent(name: String, creatorId: String): String {
        var code: String
        do {
            code = EventCodeGenerator.generate()
            val exists = doesEventExist(code)
        } while (exists)

        val eventData = hashMapOf(
            "name" to name,
            "creatorId" to creatorId,
            "createdAt" to Timestamp.now()
        )

        eventsCollection.document(code).set(eventData).await()
        return code
    }

    suspend fun getEvent(code: String): Event? {
        val doc = eventsCollection.document(code).get().await()
        if (!doc.exists()) return null
        return doc.toEvent(code)
    }

    fun observeEvent(code: String): Flow<Event?> = callbackFlow {
        val listener = eventsCollection.document(code)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val event = snapshot?.takeIf { it.exists() }?.toEvent(code)
                trySend(event)
            }
        awaitClose { listener.remove() }
    }

    fun observeParticipants(code: String): Flow<List<Participant>> = callbackFlow {
        val listener = eventsCollection.document(code)
            .collection("participants")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val participants = snapshot?.documents?.map { doc ->
                    Participant(
                        userId = doc.id,
                        name = doc.getString("name") ?: "",
                        availableDates = (doc.get("availableDates") as? List<*>)
                            ?.filterIsInstance<Timestamp>()
                            ?: emptyList(),
                        notes = (doc.get("notes") as? List<*>)?.filterIsInstance<String>()
                            ?: listOfNotNull(doc.getString("note")?.takeIf { it.isNotBlank() })
                    )
                } ?: emptyList()
                trySend(participants)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addOrUpdateAvailability(
        code: String,
        userId: String,
        name: String,
        dates: List<LocalDate>,
        notes: List<String> = emptyList()
    ) {
        val today = LocalDate.now()
        val data = hashMapOf(
            "name" to name,
            "availableDates" to dates.filter { !it.isBefore(today) }.map { it.toTimestamp() },
            "notes" to notes
        )
        eventsCollection.document(code)
            .collection("participants")
            .document(userId)
            .set(data)
            .await()
    }

    suspend fun getEvents(codes: Collection<String>): List<Event> {
        return codes.mapNotNull { getEvent(it) }
    }

    suspend fun deleteEvent(code: String) {
        val eventRef = eventsCollection.document(code)
        // Delete all participants first
        val participants = eventRef.collection("participants").get().await()
        participants.documents.forEach { it.reference.delete().await() }
        eventRef.delete().await()
    }

    suspend fun doesEventExist(code: String): Boolean {
        val doc = eventsCollection.document(code).get().await()
        return doc.exists()
    }

    private fun LocalDate.toTimestamp(): Timestamp {
        val instant = this.atStartOfDay(ZoneOffset.UTC).toInstant()
        return Timestamp(Date.from(instant))
    }

    /**
     * Fija el día de la sesión y, opcionalmente, la hora de inicio.
     * Pasar `startTime = null` deja la hora sin tocar (p. ej. al cambiar solo el día).
     */
    suspend fun confirmDate(code: String, date: LocalDate, startTime: LocalTime? = null) {
        val updates = mutableMapOf<String, Any?>("confirmedDate" to date.toTimestamp())
        if (startTime != null) updates["startTime"] = startTime.format(TIME_FORMAT)
        eventsCollection.document(code).update(updates).await()
    }

    suspend fun setStartTime(code: String, startTime: LocalTime?) {
        eventsCollection.document(code)
            .update("startTime", startTime?.format(TIME_FORMAT))
            .await()
    }

    suspend fun clearConfirmedDate(code: String) {
        eventsCollection.document(code)
            .update(mapOf("confirmedDate" to null, "startTime" to null))
            .await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toEvent(code: String): Event {
        return Event(
            code = code,
            name = getString("name") ?: "",
            creatorId = getString("creatorId") ?: "",
            createdAt = getTimestamp("createdAt") ?: Timestamp.now(),
            confirmedDate = getTimestamp("confirmedDate"),
            startTime = getString("startTime")
        )
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
