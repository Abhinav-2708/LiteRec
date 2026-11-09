package com.example.model

data class RecordingState(
    val isRecording: Boolean = false,
    val isPreparing: Boolean = false,
    val durationMs: Long = 0L,
    val config: RecordingConfig = RecordingConfig(),
    val audioRecordingActive: Boolean = false,
    val audioErrorMessage: String? = null,
    val recordingWidth: Int = 0,
    val recordingHeight: Int = 0,
    val latestSavedUri: String? = null,
    val latestSavedFileName: String? = null,
    val errorMessage: String? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}
