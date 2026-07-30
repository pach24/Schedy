package com.schednd.data.work

import dagger.hilt.android.qualifiers.ApplicationContext
import com.schednd.domain.repository.SessionReminderScheduler
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import com.schednd.R

/**
 * Programa un aviso local el día antes de la sesión.
 *
 * Es único por código de evento (`REPLACE`), así que reconfirmar una fecha
 * reprograma en vez de acumular avisos.
 */
@Singleton
class SessionReminderSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionReminderScheduler {
    override fun schedule(
        code: String,
        sessionName: String,
        date: LocalDate,
        startTime: LocalTime?,
        now: LocalDateTime
    ) {
        // Sin hora fijada avisamos a una hora civilizada de la tarde anterior.
        val remindAt = date.minusDays(1).atTime(startTime ?: DEFAULT_REMINDER_TIME)
        val delay = Duration.between(now, remindAt)
        if (delay.isNegative || delay.isZero) {
            // La sesión es hoy o mañana ya pasada la hora del aviso: nada que programar.
            cancel(code)
            return
        }

        // Se compone al programar, con el idioma de ese momento. El aviso salta como
        // mucho un dia despues, asi que un cambio de idioma por medio es despreciable.
        val whenText = if (startTime != null) {
            context.getString(R.string.notification_reminder_at, startTime.format(HOUR_FORMAT))
        } else {
            context.getString(R.string.notification_reminder_no_time)
        }

        val request = OneTimeWorkRequestBuilder<SessionReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(
                Data.Builder()
                    .putString(SessionReminderWorker.KEY_CODE, code)
                    .putString(SessionReminderWorker.KEY_SESSION_NAME, sessionName)
                    .putString(SessionReminderWorker.KEY_WHEN_TEXT, whenText)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(code), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(code: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(code))
    }

    private fun workName(code: String) = "$WORK_PREFIX$code"

    private companion object {
        const val WORK_PREFIX = "session_reminder_"
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(19, 0)
        val HOUR_FORMAT: java.time.format.DateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    }
}
