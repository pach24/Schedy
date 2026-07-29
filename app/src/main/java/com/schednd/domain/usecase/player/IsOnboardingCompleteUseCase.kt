package com.schednd.domain.usecase.player

import com.schednd.domain.repository.PlayerRepository
import javax.inject.Inject

/** Decide con qué pantalla arranca la app. */
class IsOnboardingCompleteUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    operator fun invoke(): Boolean = playerRepository.isOnboardingComplete()
}
