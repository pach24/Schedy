package com.schednd.data.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.schednd.R
import com.schednd.SchedyApp

class SchedyMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data

        // Los avisos se publican al topic del evento, así que también llegan a
        // quien disparó la acción. Ese no necesita enterarse de lo que acaba de hacer.
        val senderId = data["senderId"].orEmpty()
        if (senderId.isNotEmpty() && senderId == Firebase.auth.currentUser?.uid) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val title = data["title"] ?: message.notification?.title ?: "Schedy"
        val body = data["body"] ?: message.notification?.body
            ?: "Novedades en tu sesión"

        val builder = NotificationCompat.Builder(this, SchedyApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)

        data["code"]?.takeIf { it.isNotBlank() }?.let { code ->
            builder.setContentIntent(sessionIntent(code))
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun sessionIntent(code: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("schedy://event/$code")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            code.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Los envíos van por topic (`event_{code}`), no hace falta guardar el token.
    }
}
