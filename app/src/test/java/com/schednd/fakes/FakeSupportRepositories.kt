package com.schednd.fakes

import com.schednd.domain.repository.AuthRepository
import com.schednd.domain.repository.MessagingRepository
import com.schednd.domain.repository.PlayerRepository
import com.schednd.domain.repository.RecentEventsRepository

/** Sesión anónima siempre resuelta, con un uid fijo para poder afirmarlo. */
class FakeAuthRepository(private val uid: String? = "uid-1") : AuthRepository {
    var ensureSignedInCalls: Int = 0
        private set

    override fun getCurrentUserId(): String? = uid

    override suspend fun ensureSignedIn(): String {
        ensureSignedInCalls++
        return uid ?: error("sin sesión")
    }
}

class FakePlayerRepository(private var name: String? = null) : PlayerRepository {
    override fun getPlayerName(): String? = name
    override fun savePlayerName(name: String) {
        this.name = name
    }
    override fun isOnboardingComplete(): Boolean = !name.isNullOrBlank()
}

class FakeRecentEventsRepository(codes: List<String> = emptyList()) : RecentEventsRepository {
    private val saved = codes.toMutableList()
    override fun saveEvent(code: String) {
        if (code !in saved) saved += code
    }
    override fun removeEvent(code: String) {
        saved -= code
    }
    override fun getSavedCodes(): List<String> = saved.toList()
}

class FakeMessagingRepository : MessagingRepository {
    val subscribed: MutableList<String> = mutableListOf()
    override suspend fun subscribeToEvent(code: String) {
        subscribed += code
    }
    override suspend fun unsubscribeFromEvent(code: String) {
        subscribed -= code
    }
}
