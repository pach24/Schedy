package com.schednd.domain.usecase.session

import com.schednd.domain.model.Event
import com.schednd.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(code: String): Flow<Event?> = eventRepository.observeEvent(code)
}
