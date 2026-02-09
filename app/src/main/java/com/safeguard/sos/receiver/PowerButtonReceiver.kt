package com.safeguard.sos.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.safeguard.sos.data.local.datastore.UserPreferences
import com.safeguard.sos.service.sos.SOSService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PowerButtonReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userPreferences: UserPreferences

    companion object {
        private const val PRESS_COUNT_THRESHOLD = 5
        private const val PRESS_TIME_WINDOW = 3000L // 3 seconds

        private var lastPressTime = 0L
        private var pressCount = 0
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF ||
            intent.action == Intent.ACTION_SCREEN_ON) {

            val currentTime = System.currentTimeMillis()

            if (currentTime - lastPressTime < PRESS_TIME_WINDOW) {
                pressCount++
            } else {
                pressCount = 1
            }

            lastPressTime = currentTime

            if (pressCount >= PRESS_COUNT_THRESHOLD) {
                pressCount = 0

                CoroutineScope(Dispatchers.IO).launch {
                    val isPowerButtonSOSEnabled = userPreferences.powerButtonSOSFlow.first()

                    if (isPowerButtonSOSEnabled) {
                        triggerSOS(context)
                    }
                }
            }
        }
    }

    private fun triggerSOS(context: Context) {
        val sosIntent = Intent(context, SOSService::class.java).apply {
            action = SOSService.ACTION_TRIGGER_SOS
            putExtra(SOSService.EXTRA_EMERGENCY_TYPE, "SAFETY")
            putExtra(SOSService.EXTRA_SILENT_MODE, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(sosIntent)
        } else {
            context.startService(sosIntent)
        }
    }
}
