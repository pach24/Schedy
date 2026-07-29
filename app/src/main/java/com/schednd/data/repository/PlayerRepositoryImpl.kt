package com.schednd.data.repository

import com.schednd.domain.repository.PlayerRepository
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : PlayerRepository {
    private val prefs by lazy {
        context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
    }

    override fun getPlayerName(): String? = prefs.getString("player_name", null).takeIf { !it.isNullOrBlank() }

    override fun savePlayerName(name: String) {
        prefs.edit().putString("player_name", name.trim()).apply()
    }

    override fun isOnboardingComplete(): Boolean = !getPlayerName().isNullOrBlank()
}
