package com.schednd.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encola avisos en `events/{code}/pendingNotifications`.
 *
 * El envío real lo hace la Cloud Function `publishPendingNotification`, que
 * publica al topic `event_{code}` y borra el doc de la cola. `senderId` es
 * obligatorio: las reglas de Firestore exigen que coincida con el uid que
 * escribe, y el cliente lo usa para no auto-notificarse.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun queueRef(code: String) =
        firestore.collection("events").document(code).collection("pendingNotifications")

    private suspend fun enqueue(code: String, payload: Map<String, Any?>) {
        val data = payload.toMutableMap()
        data["createdAt"] = FieldValue.serverTimestamp()
        queueRef(code).add(data)
    }

    suspend fun notifyNewNote(
        code: String,
        senderId: String,
        noteId: String,
        title: String,
        authorName: String
    ) = enqueue(
        code,
        mapOf(
            "type" to "NEW_NOTE",
            "senderId" to senderId,
            "noteId" to noteId,
            "title" to title,
            "authorName" to authorName
        )
    )

    suspend fun notifyAvailabilityUpdated(
        code: String,
        senderId: String,
        senderName: String
    ) = enqueue(
        code,
        mapOf(
            "type" to "AVAILABILITY_UPDATED",
            "senderId" to senderId,
            "senderName" to senderName
        )
    )

    suspend fun notifyDateConfirmed(
        code: String,
        senderId: String,
        body: String
    ) = enqueue(
        code,
        mapOf(
            "type" to "DATE_CONFIRMED",
            "senderId" to senderId,
            "body" to body
        )
    )

    suspend fun notifyDateCleared(
        code: String,
        senderId: String
    ) = enqueue(
        code,
        mapOf(
            "type" to "DATE_CLEARED",
            "senderId" to senderId
        )
    )
}
