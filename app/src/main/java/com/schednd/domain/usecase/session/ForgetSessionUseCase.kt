package com.schednd.domain.usecase.session

import com.schednd.domain.repository.RecentEventsRepository
import javax.inject.Inject

class ForgetSessionUseCase @Inject constructor(
    private val recentEventsRepository: RecentEventsRepository
) {
    operator fun invoke(code: String) = recentEventsRepository.removeEvent(code)
}
