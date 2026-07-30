package com.schednd.domain.usecase.notification

import com.schednd.domain.repository.NotificationRepository
import javax.inject.Inject

class NotifyDateClearedUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(code: String, senderId: String) =
        notificationRepository.notifyDateCleared(code, senderId)
}
