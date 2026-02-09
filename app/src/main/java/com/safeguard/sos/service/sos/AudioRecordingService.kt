package com.safeguard.sos.service.sos

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safeguard.sos.R
import com.safeguard.sos.core.common.Constants
import com.safeguard.sos.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AudioRecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var isRecording = false
    private var recordingJob: Job? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AudioRecordingService"
        private const val NOTIFICATION_ID = 3001
        private const val MAX_RECORDING_DURATION_MS = 300000L // 5 minutes

        const val ACTION_START_RECORDING = "com.safeguard.sos.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.safeguard.sos.STOP_RECORDING"
        const val EXTRA_SOS_ID = "extra_sos_id"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val sosId = intent.getStringExtra(EXTRA_SOS_ID)
                startRecording(sosId)
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        serviceScope.cancel()
    }

    private fun startRecording(sosId: String?) {
        if (!hasRecordingPermission()) {
            Log.e(TAG, "Recording permission not granted")
            stopSelf()
            return
        }

        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        try {
            currentFilePath = createAudioFilePath(sosId)

            mediaRecorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentFilePath)

                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started: $currentFilePath")

            // Auto-stop after max duration
            recordingJob = serviceScope.launch {
                delay(MAX_RECORDING_DURATION_MS)
                stopRecording()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            stopSelf()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        recordingJob?.cancel()

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            Log.d(TAG, "Recording stopped: $currentFilePath")

            // Upload the recording
            currentFilePath?.let { path ->
                uploadRecording(path)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun uploadRecording(filePath: String) {
        serviceScope.launch {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    // TODO: Upload to server
                    Log.d(TAG, "Recording ready for upload: ${file.length()} bytes")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload recording", e)
            }
        }
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun createAudioFilePath(sosId: String?): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SOS_${sosId ?: "unknown"}_$timestamp.m4a"

        val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
        } else {
            File(getExternalFilesDir(null), "recordings")
        }

        directory?.mkdirs()
        return File(directory, fileName).absolutePath
    }

    private fun hasRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "sos_channel") // Placeholder
            .setContentTitle("Recording Audio")
            .setContentText("Emergency audio recording is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "STOP", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
