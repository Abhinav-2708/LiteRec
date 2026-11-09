package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.RecordingConfig
import com.example.recorder.ScreenRecorderEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        const val ACTION_START = "com.example.literec.action.START"
        const val ACTION_STOP = "com.example.literec.action.STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_CONFIG = "extra_config"

        private const val NOTIFICATION_CHANNEL_ID = "literec_recording_channel"
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETE_NOTIFICATION_ID = 1002

        fun startIntent(context: Context, resultCode: Int, data: Intent, config: RecordingConfig): Intent {
            return Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
                putExtra(EXTRA_CONFIG, config)
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var startTimeMs: Long = 0L

    private var isSessionActive = false
    private val sessionLock = Any()

    private var mediaProjection: MediaProjection? = null
    private var recorderEngine: ScreenRecorderEngine? = null
    private var currentConfig: RecordingConfig = RecordingConfig()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                val config: RecordingConfig? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_CONFIG, RecordingConfig::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_CONFIG) as? RecordingConfig
                }

                if (resultData != null && resultCode != 0) {
                    currentConfig = config ?: RecordingConfig()
                    startRecordingSession(resultCode, resultData, currentConfig)
                } else {
                    Log.e(TAG, "Missing MediaProjection credentials in ACTION_START")
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                stopRecordingSession()
            }
        }

        return START_NOT_STICKY
    }

    private fun startRecordingSession(resultCode: Int, data: Intent, config: RecordingConfig) {
        synchronized(sessionLock) {
            if (isSessionActive || recorderEngine != null) {
                Log.w(TAG, "Recording session is already active; ignoring duplicate start request")
                return
            }
            isSessionActive = true
        }

        // Step 1: Immediately start foreground service with type mediaProjection
        val initialNotification = buildNotification("00:00")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        // Step 2: Acquire MediaProjection token
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)
        if (projection == null) {
            Log.e(TAG, "Failed to obtain MediaProjection from token")
            RecordingStateController.onError("Could not acquire screen capture permission")
            stopForeground(STOP_FOREGROUND_REMOVE)
            synchronized(sessionLock) { isSessionActive = false }
            stopSelf()
            return
        }
        mediaProjection = projection

        // Step 3: Initialize and launch the screen recorder engine
        var isAudioActive = false
        var audioError: String? = null

        val engine = ScreenRecorderEngine(
            context = applicationContext,
            mediaProjection = projection,
            config = config,
            onAudioStatusChanged = { active, error ->
                isAudioActive = active
                audioError = error
                RecordingStateController.updateAudioStatus(active, error)
            },
            onError = { errMsg ->
                Log.e(TAG, "Recorder error: $errMsg")
                RecordingStateController.onError(errMsg)
                stopRecordingSession()
            }
        )

        val outputUri = engine.start()
        if (outputUri == null) {
            Log.e(TAG, "ScreenRecorderEngine failed to initialize output")
            stopRecordingSession()
            return
        }
        recorderEngine = engine
        startTimeMs = SystemClock.elapsedRealtime()

        RecordingStateController.onRecordingStarted(
            config = config,
            width = engine.currentWidth,
            height = engine.currentHeight,
            audioActive = isAudioActive,
            audioErrorMessage = audioError
        )

        // Step 4: Start lightweight 1-second interval ticker for duration and notification update
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val elapsed = SystemClock.elapsedRealtime() - startTimeMs
                RecordingStateController.updateDuration(elapsed)

                val formatted = formatDuration(elapsed)
                val updatedNotification = buildNotification(formatted)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            }
        }
    }

    private fun stopRecordingSession() {
        val engine: ScreenRecorderEngine?
        synchronized(sessionLock) {
            if (!isSessionActive && recorderEngine == null) {
                Log.w(TAG, "stopRecordingSession called but session is not active")
                return
            }
            isSessionActive = false
            engine = recorderEngine
            recorderEngine = null
            mediaProjection = null
        }

        timerJob?.cancel()
        timerJob = null

        val (savedUri, fileName) = engine?.stop() ?: Pair(null, null)
        val uriString = savedUri?.toString()

        RecordingStateController.onRecordingStopped(uriString, fileName)

        stopForeground(STOP_FOREGROUND_REMOVE)

        if (savedUri != null) {
            showCompletionNotification(fileName ?: "LiteRec.mp4")
        }

        stopSelf()
    }

    private fun buildNotification(durationText: String): Notification {
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val audioStatus = if (currentConfig.recordInternalAudio) "Playback Audio" else "Audio Muted"
        val subtitle = "$durationText • ${currentConfig.fps} FPS • $audioStatus"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_record)
            .setContentTitle(getString(R.string.notification_title_recording))
            .setContentText(subtitle)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun showCompletionNotification(fileName: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_record)
            .setContentTitle(getString(R.string.notification_saved_title))
            .setContentText("$fileName (${getString(R.string.notification_saved_msg)})")
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(COMPLETE_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW // Silent to avoid buzzing during recording
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        recorderEngine?.stop()
        recorderEngine = null
        mediaProjection = null
        synchronized(sessionLock) { isSessionActive = false }
        Log.d(TAG, "RecordingService destroyed.")
    }
}
