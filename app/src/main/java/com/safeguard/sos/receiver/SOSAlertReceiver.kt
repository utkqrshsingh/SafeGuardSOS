package com.safeguard.sos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.safeguard.sos.service.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SOSAlertReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_RESPOND -> {
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID) ?: return
                // Handle respond action
            }
            ACTION_DISMISS -> {
                val alertId = intent.getStringExtra(EXTRA_ALERT_ID) ?: return
                notificationHelper.cancelNotification(alertId.hashCode())
            }
            ACTION_HELPER_RESPONDING -> {
                val alertId = intent.getStringExtra(EXTRA_SOS_ID) ?: return
                val helperName = intent.getStringExtra(EXTRA_HELPER_NAME) ?: "A helper"
                val eta = intent.getStringExtra(EXTRA_ETA)
                notificationHelper.showHelperRespondingNotification(alertId, helperName, eta)
            }
        }
    }

    companion object {
        const val ACTION_RESPOND = "com.safeguard.sos.ACTION_RESPOND"
        const val ACTION_DISMISS = "com.safeguard.sos.ACTION_DISMISS"
        const val ACTION_HELPER_RESPONDING = "com.safeguard.sos.HELPER_RESPONDING"
        
        const val EXTRA_ALERT_ID = "extra_alert_id"
        const val EXTRA_SOS_ID = "extra_sos_id"
        const val EXTRA_HELPER_NAME = "extra_helper_name"
        const val EXTRA_ETA = "extra_eta"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
    }
}
