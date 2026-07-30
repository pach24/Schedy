package com.schednd.domain.usecase.session

import com.schednd.domain.model.Event
import com.schednd.domain.repository.EventRepository
import javax.inject.Inject

class GetSessionUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(code: String): Event? = eventRepository.getEvent(code)
}
