package com.schednd.domain.usecase.session

import com.schednd.domain.repository.MessagingRepository
import javax.inject.Inject

/** Alta en los avisos del grupo. */
class SubscribeToSessionUseCase @Inject constructor(
    private val messagingRepository: MessagingRepository
) {
    suspend operator fun invoke(code: String) = messagingRepository.subscribeToEvent(code)
}
