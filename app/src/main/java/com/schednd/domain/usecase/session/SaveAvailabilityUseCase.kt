package com.schednd.domain.usecase.session

import com.schednd.domain.repository.EventRepository
import java.time.LocalDate
import javax.inject.Inject

/** Guarda los días que puede un jugador, con su nombre por si lo ha cambiado. */
class SaveAvailabilityUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    /**
     * Los días que ya pasaron se descartan antes de escribir: marcarlos no sirve de nada y
     * el resto de la app los ignora igualmente al leer. Se filtra aquí y no en cada
     * pantalla para que crear, unirse y editar disponibilidad sigan la misma regla.
     */
    suspend operator fun invoke(
        code: String,
        userId: String,
        name: String,
        dates: List<LocalDate>,
        notes: List<String> = emptyList()
    ) {
        val today = LocalDate.now()
        eventRepository.addOrUpdateAvailability(
            code = code,
            userId = userId,
            name = name,
            dates = dates.filter { !it.isBefore(today) },
            notes = notes
        )
    }
}
