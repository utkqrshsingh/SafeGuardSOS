package com.safeguard.sos.service.notification

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safeguard.sos.R
import com.safeguard.sos.core.common.Constants
import com.safeguard.sos.data.local.datastore.UserPreferences
import com.safeguard.sos.domain.repository.UserRepository
import com.safeguard.sos.presentation.main.MainActivity
import com.safeguard.sos.receiver.SOSAlertReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FCMService"

        // Notification Types
        const val TYPE_SOS_ALERT = "sos_alert"
        const val TYPE_SOS_UPDATE = "sos_update"
        const val TYPE_SOS_CANCELLED = "sos_cancelled"
        const val TYPE_SOS_RESOLVED = "sos_resolved"
        const val TYPE_HELPER_RESPONDING = "helper_responding"
        const val TYPE_HELPER_ARRIVED = "helper_arrived"
        const val TYPE_CONTACT_SOS = "contact_sos"
        const val TYPE_GENERAL = "general"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token: $token")

        serviceScope.launch {
            try {
                // userPreferences.saveFcmToken(token) // Correct if method exists
                userRepository.updateFcmToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val data = message.data
        val notificationType = data["type"] ?: TYPE_GENERAL

        when (notificationType) {
            TYPE_SOS_ALERT -> handleSOSAlert(data)
            TYPE_SOS_UPDATE -> handleSOSUpdate(data)
            TYPE_SOS_CANCELLED -> handleSOSCancelled(data)
            TYPE_SOS_RESOLVED -> handleSOSResolved(data)
            TYPE_HELPER_RESPONDING -> handleHelperResponding(data)
            TYPE_HELPER_ARRIVED -> handleHelperArrived(data)
            TYPE_CONTACT_SOS -> handleContactSOS(data)
            else -> handleGeneralNotification(message)
        }
    }

    private fun handleSOSAlert(data: Map<String, String>) {
        val alertId = data["alert_id"] ?: return
        val userName = data["user_name"] ?: "Someone"
        val emergencyType = data["emergency_type"] ?: "Emergency"
        val distance = data["distance"] ?: "nearby"
        val latitude = data["latitude"]?.toDoubleOrNull()
        val longitude = data["longitude"]?.toDoubleOrNull()

        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_ALERT
            putExtra(MainActivity.EXTRA_ALERT_ID, alertId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, alertId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to respond
        val respondIntent = Intent(this, SOSAlertReceiver::class.java).apply {
            action = SOSAlertReceiver.ACTION_HELPER_RESPONDING
            putExtra(SOSAlertReceiver.EXTRA_SOS_ID, alertId)
        }
        val respondPendingIntent = PendingIntent.getBroadcast(
            this, alertId.hashCode() + 1, respondIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "sos_channel") // Placeholder channel
            .setContentTitle("🆘 $emergencyType Alert Nearby!")
            .setContentText("$userName needs help - $distance away")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .addAction(android.R.drawable.ic_menu_directions, "RESPOND", respondPendingIntent)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$userName needs help!\nEmergency Type: $emergencyType\nDistance: $distance\n\nTap to view details and help."))
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        try {
            NotificationManagerCompat.from(this).notify(alertId.hashCode(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }

        // Play alert sound
        notificationHelper.playAlertSound()
    }

    private fun handleSOSUpdate(data: Map<String, String>) {
        val alertId = data["alert_id"] ?: return
        val message = data["message"] ?: "Alert updated"
        val userName = data["user_name"] ?: "User"

        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_ALERT
            putExtra(MainActivity.EXTRA_ALERT_ID, alertId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, alertId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "sos_channel")
            .setContentTitle("Update from $userName")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(alertId.hashCode() + 100, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }

    private fun handleSOSCancelled(data: Map<String, String>) {
        val alertId = data["alert_id"] ?: return
        val userName = data["user_name"] ?: "User"

        // Cancel existing notification
        NotificationManagerCompat.from(this).cancel(alertId.hashCode())

        val notification = NotificationCompat.Builder(this, "sos_channel")
            .setContentTitle("Alert Cancelled")
            .setContentText("$userName has cancelled the SOS alert")
            .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(alertId.hashCode() + 200, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }

    private fun handleSOSResolved(data: Map<String, String>) {
        val alertId = data["alert_id"] ?: return
        val userName = data["user_name"] ?: "User"

        // Cancel existing notification
        NotificationManagerCompat.from(this).cancel(alertId.hashCode())

        val notification = NotificationCompat.Builder(this, "sos_channel")
            .setContentTitle("✅ Alert Resolved")
            .setContentText("$userName is now safe. Thank you for your help!")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(alertId.hashCode() + 300, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }

    private fun handleHelperResponding(data: Map<String, String>) {
        val alertId = data["alert_id"] ?: return
        val helperName = data["helper_name"] ?: "A helper"
        val eta = data["eta"] ?: "soon"

        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_ALERT
            putExtra(MainActivity.EXTRA_ALERT_ID, alertId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, alertId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "sos_channel")
            .setContentTitle("🚗 Help is on the way!")
            .setContentText("$helperName is coming to help you (ETA: $eta)")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(alertId.hashCode() + 400, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }

    private fun handleHelperArrived(data: Map<String, String>) {
        val alertId = data["alert_id"] ?: return
        val helperName = data["helper_name"] ?: "A helper"

        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_ALERT
            putExtra(MainActivity.EXTRA_ALERT_ID, alertId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, alertId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "sos_channel")
            .setContentTitle("✅ Help has arrived!")
            .setContentText("$helperName has arrived at your location")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .build()

        try {
            NotificationManagerCompat.from(this).notify(alertId.hashCode() + 500, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }

    private fun handleContactSOS(data: Map<String, String>) {
        val alertId = data["alert_id"] ?: return
        val contactName = data["contact_name"] ?: "Your contact"
        val emergencyType = data["emergency_type"] ?: "Emergency"

        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_ALERT
            putExtra(MainActivity.EXTRA_ALERT_ID, alertId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, alertId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "sos_channel")
            .setContentTitle("🆘 $contactName needs help!")
            .setContentText("$emergencyType emergency - Tap for details")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setVibrate(longArrayOf(0, 1000, 200, 1000))
            .build()

        try {
            NotificationManagerCompat.from(this).notify(alertId.hashCode(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }

        // Play emergency sound
        notificationHelper.playEmergencySound()
    }

    private fun handleGeneralNotification(message: RemoteMessage) {
        val title = message.notification?.title ?: "SafeGuard SOS"
        val body = message.notification?.body ?: ""

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "general_channel")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }
}
