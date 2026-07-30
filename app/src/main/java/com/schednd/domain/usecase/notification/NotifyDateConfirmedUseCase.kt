package com.schednd.domain.usecase.notification

import com.schednd.domain.repository.NotificationRepository
import javax.inject.Inject

class NotifyDateConfirmedUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(code: String, senderId: String, body: String) =
        notificationRepository.notifyDateConfirmed(code, senderId, body)
}
