package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LiteRecTileService : TileService() {

    companion object {
        private const val TAG = "LiteRecTileService"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateObservationJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening")

        stateObservationJob?.cancel()
        stateObservationJob = serviceScope.launch {
            RecordingStateController.state.collectLatest { state ->
                updateTileUi(state.isRecording, state.formattedDuration)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "onStopListening")
        stateObservationJob?.cancel()
        stateObservationJob = null
    }

    override fun onClick() {
        super.onClick()
        val isRecording = RecordingStateController.state.value.isRecording

        if (isRecording) {
            // Tapping active tile stops the recording immediately
            Log.d(TAG, "Tile clicked while recording: stopping session")
            val stopIntent = RecordingService.stopIntent(this)
            startService(stopIntent)
            updateTileUi(isRecording = false, durationText = "")
        } else {
            // Tapping inactive tile opens MainActivity to immediately initiate the screen capture consent flow
            Log.d(TAG, "Tile clicked while idle: opening app to request screen capture")
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_TRIGGER_RECORD_FROM_TILE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    101,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(launchIntent)
            }
        }
    }

    private fun updateTileUi(isRecording: Boolean, durationText: String) {
        val tile = qsTile ?: return

        try {
            if (isRecording) {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.quick_settings_tile_recording)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (durationText.isNotEmpty()) durationText else "Recording"
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_record)
            } else {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.quick_settings_tile_label)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to record"
                }
                tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_record)
            }
            tile.updateTile()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update QS tile state", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stateObservationJob?.cancel()
    }
}
