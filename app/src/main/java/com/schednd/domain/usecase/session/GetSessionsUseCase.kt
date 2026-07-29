package com.schednd.domain.usecase.session

import com.schednd.domain.model.Event
import com.schednd.domain.repository.EventRepository
import javax.inject.Inject

class GetSessionsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(codes: Collection<String>): List<Event> =
        eventRepository.getEvents(codes)
}
