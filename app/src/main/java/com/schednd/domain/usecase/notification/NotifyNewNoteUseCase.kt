package com.schednd.domain.usecase.notification

import com.schednd.domain.repository.NotificationRepository
import javax.inject.Inject

class NotifyNewNoteUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(
        code: String,
        senderId: String,
        noteId: String,
        title: String,
        authorName: String
    ) = notificationRepository.notifyNewNote(code, senderId, noteId, title, authorName)
}
