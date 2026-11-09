package com.example.service

import com.example.model.RecordingConfig
import com.example.model.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object RecordingStateController {
    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    fun updateConfig(config: RecordingConfig) {
        _state.update { it.copy(config = config) }
    }

    fun setPreparing(isPreparing: Boolean) {
        _state.update { it.copy(isPreparing = isPreparing, errorMessage = null) }
    }

    fun onRecordingStarted(
        config: RecordingConfig,
        width: Int,
        height: Int,
        audioActive: Boolean,
        audioErrorMessage: String?
    ) {
        _state.update {
            it.copy(
                isRecording = true,
                isPreparing = false,
                durationMs = 0L,
                config = config,
                recordingWidth = width,
                recordingHeight = height,
                audioRecordingActive = audioActive,
                audioErrorMessage = audioErrorMessage,
                errorMessage = null
            )
        }
    }

    fun updateDuration(durationMs: Long) {
        _state.update { it.copy(durationMs = durationMs) }
    }

    fun updateAudioStatus(audioActive: Boolean, errorMessage: String?) {
        _state.update {
            it.copy(
                audioRecordingActive = audioActive,
                audioErrorMessage = errorMessage
            )
        }
    }

    fun onRecordingStopped(savedUri: String?, fileName: String?) {
        _state.update {
            it.copy(
                isRecording = false,
                isPreparing = false,
                durationMs = 0L,
                latestSavedUri = savedUri,
                latestSavedFileName = fileName
            )
        }
    }

    fun onError(message: String) {
        _state.update {
            it.copy(
                isRecording = false,
                isPreparing = false,
                errorMessage = message
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
