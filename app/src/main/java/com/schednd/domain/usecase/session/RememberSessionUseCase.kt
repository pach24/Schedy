package com.schednd.domain.usecase.session

import com.schednd.domain.repository.RecentEventsRepository
import javax.inject.Inject

class RememberSessionUseCase @Inject constructor(
    private val recentEventsRepository: RecentEventsRepository
) {
    operator fun invoke(code: String) = recentEventsRepository.saveEvent(code)
}
