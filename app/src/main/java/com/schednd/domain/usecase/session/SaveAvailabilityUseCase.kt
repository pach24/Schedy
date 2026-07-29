package com.schednd.domain.usecase.session

import com.schednd.domain.repository.EventRepository
import java.time.LocalDate
import javax.inject.Inject

/** Guarda los días que puede un jugador, con su nombre por si lo ha cambiado. */
class SaveAvailabilityUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        code: String,
        userId: String,
        name: String,
        dates: List<LocalDate>,
        notes: List<String> = emptyList()
    ) = eventRepository.addOrUpdateAvailability(code, userId, name, dates, notes)
}
