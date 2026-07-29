package com.schednd.domain.repository

/**
 * Cola de avisos para el grupo. El cliente solo encola: quien publica el push es una
 * Cloud Function, que es la única que puede leer esta colección.
 */
interface NotificationRepository {

    suspend fun notifyNewNote(
        code: String,
        senderId: String,
        noteId: String,
        title: String,
        authorName: String
    )

    suspend fun notifyAvailabilityUpdated(code: String, senderId: String, senderName: String)

    suspend fun notifyDateConfirmed(code: String, senderId: String, body: String)

    suspend fun notifyDateCleared(code: String, senderId: String)
}
