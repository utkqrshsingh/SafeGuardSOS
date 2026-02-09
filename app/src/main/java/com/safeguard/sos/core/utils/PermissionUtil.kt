// app/src/main/java/com/safeguard/sos/core/utils/PermissionUtil.kt

package com.safeguard.sos.core.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Utility class for handling runtime permissions
 */
object PermissionUtil {

    /**
     * Location permissions
     */
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * Background location permission (Android 10+)
     */
    val BACKGROUND_LOCATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
        emptyArray()
    }

    /**
     * Phone permissions
     */
    val PHONE_PERMISSIONS = arrayOf(
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE
    )

    /**
     * SMS permission
     */
    val SMS_PERMISSIONS = arrayOf(
        Manifest.permission.SEND_SMS
    )

    /**
     * Camera permission
     */
    val CAMERA_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    /**
     * Microphone permission
     */
    val MICROPHONE_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    /**
     * Contacts permission
     */
    val CONTACTS_PERMISSIONS = arrayOf(
        Manifest.permission.READ_CONTACTS
    )

    /**
     * Notification permission (Android 13+)
     */
    val NOTIFICATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

    /**
     * Storage permissions based on Android version
     */
    val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    /**
     * All essential permissions for SOS functionality
     */
    fun getEssentialPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        permissions.addAll(LOCATION_PERMISSIONS)
        permissions.addAll(PHONE_PERMISSIONS)
        permissions.addAll(SMS_PERMISSIONS)
        permissions.addAll(MICROPHONE_PERMISSIONS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    /**
     * Check if a permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all permissions are granted
     */
    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { isPermissionGranted(context, it) }
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return arePermissionsGranted(context, LOCATION_PERMISSIONS)
    }

    /**
     * Check if background location permission is granted
     */
    fun hasBackgroundLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isPermissionGranted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }
    }

    /**
     * Check if phone permissions are granted
     */
    fun hasPhonePermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.CALL_PHONE)
    }

    /**
     * Check if SMS permission is granted
     */
    fun hasSmsPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.SEND_SMS)
    }

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.CAMERA)
    }

    /**
     * Check if microphone permission is granted
     */
    fun hasMicrophonePermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Check if contacts permission is granted
     */
    fun hasContactsPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.READ_CONTACTS)
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    /**
     * Check if should show rationale for a permission
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Check if should show rationale (Fragment)
     */
    fun shouldShowRationale(fragment: Fragment, permission: String): Boolean {
        return fragment.shouldShowRequestPermissionRationale(permission)
    }

    /**
     * Request permissions from Activity
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * Request permissions from Fragment
     */
    fun requestPermissions(fragment: Fragment, permissions: Array<String>, requestCode: Int) {
        fragment.requestPermissions(permissions, requestCode)
    }

    /**
     * Get denied permissions from result
     */
    fun getDeniedPermissions(
        permissions: Array<String>,
        grantResults: IntArray
    ): List<String> {
        val denied = mutableListOf<String>()
        permissions.forEachIndexed { index, permission ->
            if (grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED) {
                denied.add(permission)
            }
        }
        return denied
    }

    /**
     * Check if all permissions in result are granted
     */
    fun allPermissionsGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

    /**
     * Get permission name for display
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Location"
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Background Location"
            Manifest.permission.CALL_PHONE -> "Phone Calls"
            Manifest.permission.READ_PHONE_STATE -> "Phone State"
            Manifest.permission.SEND_SMS -> "SMS"
            Manifest.permission.READ_CONTACTS -> "Contacts"
            Manifest.permission.CAMERA -> "Camera"
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage"
            Manifest.permission.READ_MEDIA_IMAGES -> "Photos"
            Manifest.permission.READ_MEDIA_VIDEO -> "Videos"
            Manifest.permission.READ_MEDIA_AUDIO -> "Audio"
            else -> permission.substringAfterLast(".")
        }
    }

    /**
     * Get permission description
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION ->
                "Required to share your location during emergencies and notify nearby helpers."
            Manifest.permission.ACCESS_BACKGROUND_LOCATION ->
                "Required to track your location during active SOS even when app is minimized."
            Manifest.permission.CALL_PHONE ->
                "Required to make emergency calls directly from the app."
            Manifest.permission.SEND_SMS ->
                "Required to send SMS alerts when internet is unavailable."
            Manifest.permission.READ_CONTACTS ->
                "Required to import emergency contacts from your phone."
            Manifest.permission.CAMERA ->
                "Required to record video during emergencies and update profile photo."
            Manifest.permission.RECORD_AUDIO ->
                "Required to record audio during emergencies for evidence."
            Manifest.permission.POST_NOTIFICATIONS ->
                "Required to receive SOS alerts and important notifications."
            else -> "Required for app functionality."
        }
    }
}