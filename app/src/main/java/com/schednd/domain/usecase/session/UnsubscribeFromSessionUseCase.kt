package com.schednd.domain.usecase.session

import com.schednd.domain.repository.MessagingRepository
import javax.inject.Inject

class UnsubscribeFromSessionUseCase @Inject constructor(
    private val messagingRepository: MessagingRepository
) {
    suspend operator fun invoke(code: String) = messagingRepository.unsubscribeFromEvent(code)
}
