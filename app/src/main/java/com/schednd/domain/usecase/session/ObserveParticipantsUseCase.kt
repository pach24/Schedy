package com.schednd.domain.usecase.session

import com.schednd.domain.model.Participant
import com.schednd.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveParticipantsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(code: String): Flow<List<Participant>> =
        eventRepository.observeParticipants(code)
}
