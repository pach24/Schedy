package com.schednd.domain.repository

/** El nombre con el que el jugador se presenta al grupo. */
interface PlayerRepository {

    fun getPlayerName(): String?

    fun savePlayerName(name: String)

    fun isOnboardingComplete(): Boolean
}
