package com.schednd.data.repository

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentEventsRepository @Inject constructor(
    private val context: Context
) {
    private val prefs by lazy {
        context.getSharedPreferences("recent_events", Context.MODE_PRIVATE)
    }

    fun saveEvent(code: String) {
        val current = getSavedCodes().toMutableList()
        current.remove(code)
        current.add(0, code) // más reciente primero
        prefs.edit().putString("codes_ordered", current.joinToString(",")).apply()
    }

    fun removeEvent(code: String) {
        val current = getSavedCodes().toMutableList()
        current.remove(code)
        prefs.edit().putString("codes_ordered", current.joinToString(",")).apply()
    }

    fun getSavedCodes(): List<String> {
        val ordered = prefs.getString("codes_ordered", null)
        if (ordered != null) {
            return if (ordered.isBlank()) emptyList() else ordered.split(",")
        }
        // Migración desde formato antiguo (StringSet)
        val legacy = prefs.getStringSet("codes", emptySet()) ?: emptySet()
        if (legacy.isNotEmpty()) {
            val list = legacy.toList()
            prefs.edit().putString("codes_ordered", list.joinToString(",")).apply()
            return list
        }
        return emptyList()
    }
}
