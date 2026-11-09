package com.example.model

import java.io.Serializable

enum class ResolutionPreset(val label: String, val targetShortDimension: Int) : Serializable {
    HD_720P("720p HD (Low Lag)", 720),
    FHD_1080P("1080p Full HD", 1080),
    NATIVE_DEVICE("Native Screen", 0)
}

data class RecordingConfig(
    val resolutionPreset: ResolutionPreset = ResolutionPreset.HD_720P,
    val fps: Int = 30,
    val bitrateMbps: Int = 4,
    val recordInternalAudio: Boolean = true,
    val countdownSeconds: Int = 0
) : Serializable {
    val bitrateBps: Int
        get() = bitrateMbps * 1_000_000

    fun calculateDimensions(screenWidth: Int, screenHeight: Int): Pair<Int, Int> {
        val isPortrait = screenHeight >= screenWidth
        val shortDim = if (isPortrait) screenWidth else screenHeight
        val longDim = if (isPortrait) screenHeight else screenWidth

        val targetShort = when (resolutionPreset) {
            ResolutionPreset.HD_720P -> 720
            ResolutionPreset.FHD_1080P -> 1080
            ResolutionPreset.NATIVE_DEVICE -> shortDim
        }

        val scale = if (shortDim > 0 && targetShort > 0) {
            (targetShort.toFloat() / shortDim.toFloat()).coerceAtMost(1.0f)
        } else 1.0f

        var outShort = (shortDim * scale).toInt()
        var outLong = (longDim * scale).toInt()

        // H.264 hardware encoders strictly require dimensions to be even numbers (multiples of 2 or 16)
        outShort = (outShort / 2) * 2
        outLong = (outLong / 2) * 2

        return if (isPortrait) {
            Pair(outShort, outLong)
        } else {
            Pair(outLong, outShort)
        }
    }
}
