package com.safeguard.sos.service.sos

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safeguard.sos.R
import com.safeguard.sos.core.common.Constants
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.SOSStatus
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.presentation.main.MainActivity
import com.safeguard.sos.service.location.LocationService
import com.safeguard.sos.service.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SOSService : Service() {

    @Inject
    lateinit var sosRepository: SOSRepository

    @Inject
    lateinit var emergencyContactRepository: EmergencyContactRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var sosManager: SOSManager

    private val binder = SOSBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alertJob: Job? = null

    private val _sosState = MutableStateFlow<SOSServiceState>(SOSServiceState.Idle)
    val sosState: StateFlow<SOSServiceState> = _sosState.asStateFlow()

    private var currentSOSId: String? = null
    private var isSilentMode = false

    companion object {
        private const val NOTIFICATION_ID = 2001

        const val ACTION_TRIGGER_SOS = "com.safeguard.sos.TRIGGER_SOS"
        const val ACTION_CANCEL_SOS = "com.safeguard.sos.CANCEL_SOS"
        const val ACTION_MARK_SAFE = "com.safeguard.sos.MARK_SAFE"
        const val ACTION_UPDATE_LOCATION = "com.safeguard.sos.UPDATE_LOCATION"

        const val EXTRA_EMERGENCY_TYPE = "extra_emergency_type"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_SILENT_MODE = "extra_silent_mode"
        const val EXTRA_CONTACTS_ONLY = "extra_contacts_only"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"

        private val VIBRATION_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500)
    }

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TRIGGER_SOS -> {
                val emergencyType = intent.getStringExtra(EXTRA_EMERGENCY_TYPE) ?: "SAFETY"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
                isSilentMode = intent.getBooleanExtra(EXTRA_SILENT_MODE, false)
                val contactsOnly = intent.getBooleanExtra(EXTRA_CONTACTS_ONLY, false)
                val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)

                triggerSOS(emergencyType, message, contactsOnly, latitude, longitude)
            }
            ACTION_CANCEL_SOS -> cancelSOS()
            ACTION_MARK_SAFE -> markSafe()
            ACTION_UPDATE_LOCATION -> {
                val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
                val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
                updateLocation(latitude, longitude)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlertSound()
        stopVibration()
        alertJob?.cancel()
        serviceScope.cancel()
    }

    inner class SOSBinder : Binder() {
        fun getService(): SOSService = this@SOSService
    }

    private fun triggerSOS(
        emergencyType: String,
        message: String,
        contactsOnly: Boolean,
        latitude: Double,
        longitude: Double
    ) {
        _sosState.value = SOSServiceState.Triggering

        // Start foreground service
        val notification = createSOSActiveNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Enable SOS mode in location service
        val locationIntent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_SOS_MODE
        }
        startService(locationIntent)

        // Start alert sound and vibration if not silent
        if (!isSilentMode) {
            startAlertSound()
            startVibration()
        }

        alertJob = serviceScope.launch {
            try {
                // Create SOS alert
                val location = Location(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = null,
                    timestamp = System.currentTimeMillis(),
                    address = null
                )

                val result = sosManager.createSOSAlert(
                    location = location,
                    emergencyType = emergencyType,
                    message = message,
                    contactsOnly = contactsOnly
                )

                result.fold(
                    onSuccess = { sosAlert ->
                        currentSOSId = sosAlert.id
                        _sosState.value = SOSServiceState.Active(sosAlert)

                        // Stop alarm after 30 seconds
                        delay(30000)
                        stopAlertSound()
                        stopVibration()

                        // Start monitoring for updates
                        monitorSOSStatus(sosAlert.id)
                    },
                    onFailure = { error ->
                        _sosState.value = SOSServiceState.Error(error.message ?: "Failed to trigger SOS")
                        stopSelf()
                    }
                )
            } catch (e: Exception) {
                _sosState.value = SOSServiceState.Error(e.message ?: "Unknown error")
                stopSelf()
            }
        }
    }

    private fun monitorSOSStatus(sosId: String) {
        serviceScope.launch {
            sosRepository.observeSOSAlert(sosId).collect { result ->
                result.getOrNull()?.let { alert ->
                    _sosState.value = SOSServiceState.Active(alert)

                    // Update notification based on status
                    val notification = when (alert.status) {
                        SOSStatus.RESPONDED -> createHelperRespondingNotification(alert)
                        SOSStatus.HELP_ON_WAY -> createHelperArrivedNotification(alert) // Placeholder status
                        SOSStatus.RESOLVED, SOSStatus.CANCELLED -> {
                            stopSelf()
                            return@collect
                        }
                        else -> createSOSActiveNotification()
                    }

                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }

    private fun cancelSOS() {
        val sosId = currentSOSId ?: return

        serviceScope.launch {
            sosManager.cancelSOSAlert(sosId)
            _sosState.value = SOSServiceState.Cancelled
            cleanup()
        }
    }

    private fun markSafe() {
        val sosId = currentSOSId ?: return

        serviceScope.launch {
            sosManager.resolveSOSAlert(sosId)
            _sosState.value = SOSServiceState.Resolved
            cleanup()
        }
    }

    private fun updateLocation(latitude: Double, longitude: Double) {
        val sosId = currentSOSId ?: return

        serviceScope.launch {
            val location = Location(
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis()
            )
            sosManager.updateSOSLocation(sosId, location)
        }
    }

    private fun cleanup() {
        stopAlertSound()
        stopVibration()

        // Disable SOS mode in location service
        val locationIntent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_NORMAL_MODE
        }
        startService(locationIntent)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startAlertSound() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                // Using standard alert sound as fallback
                setDataSource(
                    this@SOSService,
                    Uri.parse("android.resource://${packageName}/raw/sos_alert_sound")
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlertSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    private fun startVibration() {
        vibrator?.let { v ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(
                    VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(VIBRATION_PATTERN, 0)
            }
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun createSOSActiveNotification(): Notification {
        val cancelIntent = Intent(this, SOSService::class.java).apply {
            action = ACTION_CANCEL_SOS
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safeIntent = Intent(this, SOSService::class.java).apply {
            action = ACTION_MARK_SAFE
        }
        val safePendingIntent = PendingIntent.getService(
            this, 2, safeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_ALERT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "sos_channel") // Placeholder channel
            .setContentTitle("SOS ACTIVE")
            .setContentText("Your SOS alert is active")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(android.R.drawable.ic_menu_save, "I'M SAFE", safePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "CANCEL", cancelPendingIntent)
            .build()
    }

    private fun createHelperRespondingNotification(alert: SOSAlert): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_ALERT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "sos_channel")
            .setContentTitle("Help on the Way")
            .setContentText("A helper is responding to your SOS")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createHelperArrivedNotification(alert: SOSAlert): Notification {
        val safeIntent = Intent(this, SOSService::class.java).apply {
            action = ACTION_MARK_SAFE
        }
        val safePendingIntent = PendingIntent.getService(
            this, 2, safeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_VIEW_ALERT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "sos_channel")
            .setContentTitle("Help Arrived")
            .setContentText("A helper has arrived at your location")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_save, "I'M SAFE", safePendingIntent)
            .build()
    }
}

sealed class SOSServiceState {
    object Idle : SOSServiceState()
    object Triggering : SOSServiceState()
    data class Active(val alert: SOSAlert) : SOSServiceState()
    object Cancelled : SOSServiceState()
    object Resolved : SOSServiceState()
    data class Error(val message: String) : SOSServiceState()
}
