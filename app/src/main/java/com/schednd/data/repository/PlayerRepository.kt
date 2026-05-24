package com.schednd.data.repository

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
    }

    fun getPlayerName(): String? = prefs.getString("player_name", null).takeIf { !it.isNullOrBlank() }

    fun savePlayerName(name: String) {
        prefs.edit().putString("player_name", name.trim()).apply()
    }

    fun isOnboardingComplete(): Boolean = !getPlayerName().isNullOrBlank()
}
