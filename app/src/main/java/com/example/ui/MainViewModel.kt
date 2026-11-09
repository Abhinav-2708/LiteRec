package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RecordingsRepository
import com.example.model.RecordingConfig
import com.example.model.RecordingItem
import com.example.model.RecordingState
import com.example.service.RecordingService
import com.example.service.RecordingStateController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    DASHBOARD,
    RECORDINGS,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecordingsRepository(application)

    val recordingState: StateFlow<RecordingState> = RecordingStateController.state

    private val _selectedTab = MutableStateFlow(AppTab.DASHBOARD)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _recordings = MutableStateFlow<List<RecordingItem>>(emptyList())
    val recordings: StateFlow<List<RecordingItem>> = _recordings.asStateFlow()

    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    private val _showTileGuide = MutableStateFlow(false)
    val showTileGuide: StateFlow<Boolean> = _showTileGuide.asStateFlow()

    private val _previewVideo = MutableStateFlow<RecordingItem?>(null)
    val previewVideo: StateFlow<RecordingItem?> = _previewVideo.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadRecordings()

        viewModelScope.launch {
            RecordingStateController.state.collectLatest { state ->
                if (!state.isRecording && state.latestSavedUri != null) {
                    // Refresh recordings when a new recording completes
                    delay(400)
                    loadRecordings()
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setShowTileGuide(show: Boolean) {
        _showTileGuide.value = show
    }

    fun setPreviewVideo(item: RecordingItem?) {
        _previewVideo.value = item
    }

    fun updateConfig(config: RecordingConfig) {
        RecordingStateController.updateConfig(config)
    }

    fun loadRecordings() {
        viewModelScope.launch {
            val list = repository.loadRecordings()
            _recordings.value = list
        }
    }

    fun deleteRecording(item: RecordingItem) {
        viewModelScope.launch {
            val success = repository.deleteRecording(item)
            if (success) {
                _recordings.update { current -> current.filter { it.id != item.id } }
                if (_previewVideo.value?.id == item.id) {
                    _previewVideo.value = null
                }
            }
        }
    }

    fun startCountdownThen(onFinish: () -> Unit) {
        val seconds = recordingState.value.config.countdownSeconds
        if (seconds <= 0) {
            onFinish()
            return
        }

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _countdown.value = i
                delay(1000L)
            }
            _countdown.value = null
            onFinish()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        _countdown.value = null
    }

    fun stopRecording() {
        val context = getApplication<Application>()
        context.startService(RecordingService.stopIntent(context))
    }

    fun clearError() {
        RecordingStateController.clearError()
    }
}
