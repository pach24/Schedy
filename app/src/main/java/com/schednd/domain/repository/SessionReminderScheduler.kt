package com.schednd.domain.repository

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Recordatorio local de la víspera. Es un puerto más del dominio: quién lo programa
 * (WorkManager, alarmas…) es cosa de data.
 */
interface SessionReminderScheduler {

    fun schedule(
        code: String,
        sessionName: String,
        date: LocalDate,
        startTime: LocalTime?,
        now: LocalDateTime = LocalDateTime.now()
    )

    fun cancel(code: String)
}
