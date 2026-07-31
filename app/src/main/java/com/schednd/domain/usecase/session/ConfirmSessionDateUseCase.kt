package com.schednd.domain.usecase.session

import com.schednd.domain.repository.EventRepository
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class ConfirmSessionDateUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    /**
     * Fijar la sesión en un día que ya pasó no significa nada. La lista de recomendadas ya
     * solo ofrece días de hoy en adelante, pero la regla vive aquí para que valga venga la
     * llamada de donde venga: esconder el día en la UI no es lo mismo que prohibirlo.
     */
    suspend operator fun invoke(code: String, date: LocalDate, startTime: LocalTime? = null) {
        require(!date.isBefore(LocalDate.now())) {
            "No se puede fijar la sesión en $date: es una fecha pasada"
        }
        eventRepository.confirmDate(code, date, startTime)
    }
}
