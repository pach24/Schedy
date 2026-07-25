package com.schednd.data.work

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.schednd.R
import com.schednd.SchedndApp

/**
 * Muestra el recordatorio local "la sesión es mañana". Se programa desde
 * [SessionReminderScheduler] al confirmar fecha y se cancela al quitarla.
 */
class SessionReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val code = inputData.getString(KEY_CODE) ?: return Result.failure()
        val sessionName = inputData.getString(KEY_SESSION_NAME).orEmpty()
        val whenText = inputData.getString(KEY_WHEN_TEXT).orEmpty()

        val granted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return Result.success()

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("schednd://event/$code")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            code.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            SchedndApp.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎲 ${sessionName.ifBlank { "Tu sesión" }} es mañana")
            .setContentText(whenText)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(code.hashCode(), notification)

        return Result.success()
    }

    companion object {
        const val KEY_CODE = "code"
        const val KEY_SESSION_NAME = "sessionName"
        const val KEY_WHEN_TEXT = "whenText"
    }
}
