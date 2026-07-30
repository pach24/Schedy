package com.schednd.domain.usecase.session

import com.schednd.domain.repository.RecentEventsRepository
import javax.inject.Inject

/** Las sesiones de este móvil: el servidor no sabe a qué mesas pertenece cada uno. */
class GetSavedSessionCodesUseCase @Inject constructor(
    private val recentEventsRepository: RecentEventsRepository
) {
    operator fun invoke(): List<String> = recentEventsRepository.getSavedCodes()
}
