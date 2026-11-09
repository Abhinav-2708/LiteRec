package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.model.RecordingConfig
import com.example.service.RecordingService
import com.example.service.RecordingStateController
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.LiteRecTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val ACTION_TRIGGER_RECORD_FROM_TILE = "com.example.literec.action.TRIGGER_RECORD_FROM_TILE"
    }

    private val viewModel: MainViewModel by viewModels()

    // Activity result launcher for MediaProjection system consent dialog
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val currentConfig = viewModel.recordingState.value.config
            startRecordingService(result.resultCode, result.data!!, currentConfig)
        } else {
            Log.w(TAG, "Screen recording permission was denied by user")
            RecordingStateController.onError("Screen recording permission denied")
            Toast.makeText(this, "Screen capture consent was cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for POST_NOTIFICATIONS (Android 13+) and RECORD_AUDIO
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] ?: true
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            grants[Manifest.permission.POST_NOTIFICATIONS] ?: true
        } else true

        if (!audioGranted && viewModel.recordingState.value.config.recordInternalAudio) {
            Toast.makeText(this, "Audio permission denied; internal playback will be muted", Toast.LENGTH_SHORT).show()
        }

        // Proceed to launch MediaProjection system prompt
        launchMediaProjectionPrompt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)

        setContent {
            LiteRecTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestStartRecording = {
                        checkPermissionsAndRequestCapture()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == ACTION_TRIGGER_RECORD_FROM_TILE) {
            Log.d(TAG, "Triggered from Quick Settings Tile")
            if (!viewModel.recordingState.value.isRecording) {
                checkPermissionsAndRequestCapture()
            }
        }
    }

    private fun checkPermissionsAndRequestCapture() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (viewModel.recordingState.value.config.recordInternalAudio) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            launchMediaProjectionPrompt()
        }
    }

    private fun launchMediaProjectionPrompt() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = projectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }

    private fun startRecordingService(resultCode: Int, data: Intent, config: RecordingConfig) {
        val serviceIntent = RecordingService.startIntent(this, resultCode, data, config)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
