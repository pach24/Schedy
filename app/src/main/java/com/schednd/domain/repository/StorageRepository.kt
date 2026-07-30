package com.schednd.domain.repository

import android.net.Uri

/** Ficheros del jugador. Aún sin usar: espera a la pantalla de perfil. */
interface StorageRepository {

    suspend fun uploadProfilePhoto(userId: String, uri: Uri): String
}
