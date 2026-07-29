package com.schednd.domain.usecase.session

import com.schednd.domain.repository.EventRepository
import javax.inject.Inject

class ClearSessionDateUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(code: String) = eventRepository.clearConfirmedDate(code)
}
