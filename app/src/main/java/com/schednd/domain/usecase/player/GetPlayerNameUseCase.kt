package com.schednd.domain.usecase.player

import com.schednd.domain.repository.PlayerRepository
import javax.inject.Inject

class GetPlayerNameUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    operator fun invoke(): String? = playerRepository.getPlayerName()
}
