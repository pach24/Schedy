package com.schednd.domain.model

import com.google.firebase.Timestamp
import java.time.LocalTime

data class Event(
    val code: String = "",
    val name: String = "",
    val creatorId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val confirmedDate: Timestamp? = null,
    /** Hora de inicio en formato "HH:mm". Null en sesiones creadas antes de existir el campo. */
    val startTime: String? = null
) {
    val startLocalTime: LocalTime?
        get() = startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
}
