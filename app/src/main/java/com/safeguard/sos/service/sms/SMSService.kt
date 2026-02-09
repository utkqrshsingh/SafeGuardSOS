package com.safeguard.sos.service.sms

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SMSService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SMSService"
        private const val ACTION_SMS_SENT = "com.safeguard.sos.SMS_SENT"
        private const val ACTION_SMS_DELIVERED = "com.safeguard.sos.SMS_DELIVERED"
        private const val MAX_SMS_LENGTH = 160
        private const val MULTIPART_SMS_LENGTH = 153
    }

    private val smsManager: SmsManager by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun sendSMS(phoneNumber: String, message: String): Boolean {
        if (!hasSmsPermission()) {
            Log.e(TAG, "SMS permission not granted")
            return false
        }

        return try {
            if (message.length <= MAX_SMS_LENGTH) {
                sendSingleSMS(phoneNumber, message)
            } else {
                sendMultipartSMS(phoneNumber, message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
            false
        }
    }

    private suspend fun sendSingleSMS(phoneNumber: String, message: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val sentIntent = PendingIntent.getBroadcast(
                context,
                phoneNumber.hashCode(),
                Intent(ACTION_SMS_SENT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    context.unregisterReceiver(this)
                    val success = resultCode == android.app.Activity.RESULT_OK
                    if (!continuation.isCompleted) {
                        continuation.resume(success)
                    }
                }
            }

            try {
                context.registerReceiver(
                    sentReceiver,
                    IntentFilter(ACTION_SMS_SENT),
                    Context.RECEIVER_NOT_EXPORTED
                )

                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    sentIntent,
                    null
                )

                Log.d(TAG, "SMS sent to $phoneNumber")
            } catch (e: Exception) {
                try {
                    context.unregisterReceiver(sentReceiver)
                } catch (_: Exception) {}
                Log.e(TAG, "Failed to send SMS", e)
                if (!continuation.isCompleted) {
                    continuation.resume(false)
                }
            }

            continuation.invokeOnCancellation {
                try {
                    context.unregisterReceiver(sentReceiver)
                } catch (_: Exception) {}
            }
        }

    private suspend fun sendMultipartSMS(phoneNumber: String, message: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val parts = smsManager.divideMessage(message)
            val sentIntents = ArrayList<PendingIntent>()
            var successCount = 0
            var failCount = 0
            val totalParts = parts.size

            val sentReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (resultCode == android.app.Activity.RESULT_OK) {
                        successCount++
                    } else {
                        failCount++
                    }

                    if (successCount + failCount >= totalParts) {
                        context.unregisterReceiver(this)
                        val success = successCount == totalParts
                        if (!continuation.isCompleted) {
                            continuation.resume(success)
                        }
                    }
                }
            }

            try {
                context.registerReceiver(
                    sentReceiver,
                    IntentFilter(ACTION_SMS_SENT),
                    Context.RECEIVER_NOT_EXPORTED
                )

                parts.forEachIndexed { index, _ ->
                    sentIntents.add(
                        PendingIntent.getBroadcast(
                            context,
                            phoneNumber.hashCode() + index,
                            Intent(ACTION_SMS_SENT),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }

                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    sentIntents,
                    null
                )

                Log.d(TAG, "Multipart SMS (${parts.size} parts) sent to $phoneNumber")
            } catch (e: Exception) {
                try {
                    context.unregisterReceiver(sentReceiver)
                } catch (_: Exception) {}
                Log.e(TAG, "Failed to send multipart SMS", e)
                if (!continuation.isCompleted) {
                    continuation.resume(false)
                }
            }

            continuation.invokeOnCancellation {
                try {
                    context.unregisterReceiver(sentReceiver)
                } catch (_: Exception) {}
            }
        }

    fun sendSOSMessage(
        phoneNumber: String,
        userName: String,
        emergencyType: String,
        latitude: Double,
        longitude: Double,
        additionalMessage: String? = null
    ): Boolean {
        val message = buildSOSMessage(userName, emergencyType, latitude, longitude, additionalMessage)

        return try {
            if (message.length <= MAX_SMS_LENGTH) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            Log.d(TAG, "SOS SMS sent to $phoneNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SOS SMS to $phoneNumber", e)
            false
        }
    }

    private fun buildSOSMessage(
        userName: String,
        emergencyType: String,
        latitude: Double,
        longitude: Double,
        additionalMessage: String?
    ): String {
        val locationUrl = "https://maps.google.com/?q=$latitude,$longitude"

        return buildString {
            append("🆘 SOS ALERT!\n")
            append("$userName needs help!\n")
            append("Type: $emergencyType\n")
            if (!additionalMessage.isNullOrBlank()) {
                append("Msg: $additionalMessage\n")
            }
            append("Location: $locationUrl")
        }
    }
}