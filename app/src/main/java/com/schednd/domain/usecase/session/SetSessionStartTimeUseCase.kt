package com.schednd.domain.usecase.session

import com.schednd.domain.repository.EventRepository
import java.time.LocalTime
import javax.inject.Inject

class SetSessionStartTimeUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(code: String, startTime: LocalTime?) =
        eventRepository.setStartTime(code, startTime)
}
