package com.schednd.domain.usecase.session

import com.schednd.domain.repository.SessionReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Recordatorio local de la víspera; reprograma solo si cambia la fecha. */
class ScheduleSessionReminderUseCase @Inject constructor(
    private val reminderScheduler: SessionReminderScheduler
) {
    operator fun invoke(
        code: String,
        sessionName: String,
        date: LocalDate,
        startTime: LocalTime?
    ) = reminderScheduler.schedule(code, sessionName, date, startTime)
}
