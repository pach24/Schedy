package com.schednd.domain.usecase.player

import com.schednd.domain.repository.PlayerRepository
import javax.inject.Inject

class SavePlayerNameUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    operator fun invoke(name: String) = playerRepository.savePlayerName(name)
}
