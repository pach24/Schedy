package com.schednd.domain.repository

/** Sesión anónima del jugador: sin uid no se puede leer ni escribir nada. */
interface AuthRepository {

    fun getCurrentUserId(): String?

    suspend fun ensureSignedIn(): String
}
