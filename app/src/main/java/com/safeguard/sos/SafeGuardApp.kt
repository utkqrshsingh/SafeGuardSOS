package com.safeguard.sos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.safeguard.sos.core.common.Constants
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SafeGuardApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        initializeFirebase()

        // Initialize Timber for logging
        initializeTimber()

        // Create notification channels
        createNotificationChannels()
    }

    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)

        // Configure Crashlytics
        if (BuildConfig.ENABLE_CRASHLYTICS) {
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
            }
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        }
    }

    private fun initializeTimber() {
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return String.format(
                        "SafeGuard:(%s:%s)#%s",
                        element.fileName,
                        element.lineNumber,
                        element.methodName
                    )
                }
            })
        } else {
            // Production tree that reports to Crashlytics
            Timber.plant(CrashReportingTree())
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // SOS Alert Channel - High Priority
            val sosChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_SOS,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency SOS notifications"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            // Helper Alert Channel - High Priority
            val helperChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_HELPER,
                "Helper Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for nearby SOS alerts"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
            }

            // Location Service Channel - Low Priority
            val locationChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_LOCATION,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location tracking service"
                setShowBadge(false)
            }

            // General Notifications Channel
            val generalChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                setShowBadge(true)
            }

            // Updates Channel
            val updatesChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_UPDATES,
                "Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "App updates and announcements"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(
                    sosChannel,
                    helperChannel,
                    locationChannel,
                    generalChannel,
                    updatesChannel
                )
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.ENABLE_LOGGING) android.util.Log.DEBUG
                else android.util.Log.ERROR
            )
            .build()

    /**
     * Custom Timber tree for production that reports to Crashlytics
     */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
                return
            }

            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log("$tag: $message")

            if (t != null) {
                if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                    crashlytics.recordException(t)
                }
            }
        }
    }
}