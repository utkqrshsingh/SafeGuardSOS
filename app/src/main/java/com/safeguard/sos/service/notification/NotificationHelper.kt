package com.safeguard.sos.service.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.safeguard.sos.R
import com.safeguard.sos.core.common.Constants
import com.safeguard.sos.presentation.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showSOSActiveNotification(sosId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Constants.ACTION_VIEW_SOS
            putExtra(Constants.EXTRA_SOS_ID, sosId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_SOS)
            .setContentTitle("SOS Active")
            .setContentText("Your SOS alert is active and being monitored")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()

        notificationManager.notify(Constants.NOTIFICATION_ID_SOS, notification)
    }

    fun showHelperAlertNotification(
        sosId: String,
        userName: String,
        distance: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = Constants.ACTION_RESPOND_SOS
            putExtra(Constants.EXTRA_SOS_ID, sosId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            sosId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_HELPER)
            .setContentTitle("🚨 New SOS Alert!")
            .setContentText("$userName needs help $distance away")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_directions,
                "RESPOND",
                pendingIntent
            )
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        notificationManager.notify(sosId.hashCode(), notification)
    }

    fun showSuccessNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_GENERAL)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showErrorNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_GENERAL)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    fun showSOSTriggeredNotification(sosId: String) {
        showSOSActiveNotification(sosId)
    }

    fun showHelperRespondingNotification(sosId: String, helperName: String, eta: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Constants.ACTION_VIEW_SOS
            putExtra(Constants.EXTRA_SOS_ID, sosId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, sosId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (eta != null) {
            "Helper $helperName is on the way (ETA: $eta)"
        } else {
            "Helper $helperName is on the way"
        }

        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_UPDATES)
            .setContentTitle("Help on the way")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(sosId.hashCode() + 1, notification)
    }

    fun playAlertSound() {
        // Placeholder
    }

    fun playEmergencySound() {
        // Placeholder
    }

    fun playSuccessSound() {
        // Placeholder
    }

    fun stopSound() {
        // Placeholder
    }
}
