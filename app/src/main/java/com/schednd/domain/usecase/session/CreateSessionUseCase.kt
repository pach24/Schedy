package com.schednd.domain.usecase.session

import com.schednd.domain.repository.EventRepository
import javax.inject.Inject

/** Crea la mesa y devuelve su código. */
class CreateSessionUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(name: String, creatorId: String): String =
        eventRepository.createEvent(name, creatorId)
}
