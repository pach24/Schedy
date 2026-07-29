package com.schednd.domain.usecase.session

import com.schednd.domain.repository.EventRepository
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class ConfirmSessionDateUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(code: String, date: LocalDate, startTime: LocalTime? = null) =
        eventRepository.confirmDate(code, date, startTime)
}
