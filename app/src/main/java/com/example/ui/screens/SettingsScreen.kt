package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordingConfig
import com.example.model.ResolutionPreset
import com.example.ui.components.OneUIHeader
import com.example.ui.theme.OneUIBackground
import com.example.ui.theme.OneUIBorder
import com.example.ui.theme.OneUIIdleGreen
import com.example.ui.theme.OneUIRecordRed
import com.example.ui.theme.OneUISurface
import com.example.ui.theme.OneUISurfaceVariant
import com.example.ui.theme.OneUITextMuted
import com.example.ui.theme.OneUITextPrimary
import com.example.ui.theme.OneUITextSecondary

@Composable
fun SettingsScreen(
    config: RecordingConfig,
    isRecording: Boolean,
    onConfigChange: (RecordingConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        OneUIHeader(
            title = "Settings",
            subtitle = "Video, Audio & Storage",
            isRecording = isRecording
        )

        if (isRecording) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = OneUISurfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = OneUITextSecondary)
                    Text(
                        text = "Recording in progress. Changes will take effect on your next session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextSecondary
                    )
                }
            }
        }

        // Section: Resolution
        SettingsSection(title = "RESOLUTION") {
            ResolutionPreset.values().forEach { preset ->
                val selected = config.resolutionPreset == preset
                SelectableOptionRow(
                    title = preset.label,
                    subtitle = when (preset) {
                        ResolutionPreset.HD_720P -> "Default • Lowest CPU & zero lag"
                        ResolutionPreset.FHD_1080P -> "Crisp high definition for large screens"
                        ResolutionPreset.NATIVE_DEVICE -> "Matches phone's full pixel grid"
                    },
                    selected = selected,
                    onClick = {
                        onConfigChange(config.copy(resolutionPreset = preset))
                    }
                )
            }
        }

        // Section: Frame Rate
        SettingsSection(title = "FRAME RATE") {
            listOf(
                30 to "30 FPS (Recommended • Minimum battery drain)",
                60 to "60 FPS (Ultra fluid • Requires higher device performance)"
            ).forEach { (fps, desc) ->
                SelectableOptionRow(
                    title = "$fps FPS",
                    subtitle = desc,
                    selected = config.fps == fps,
                    onClick = {
                        onConfigChange(config.copy(fps = fps))
                    }
                )
            }
        }

        // Section: Video Bitrate
        SettingsSection(title = "BITRATE") {
            listOf(
                4 to "4 Mbps (Balanced for 720p)",
                8 to "8 Mbps (Optimal for 1080p)",
                12 to "12 Mbps (High fidelity)"
            ).forEach { (mbps, desc) ->
                SelectableOptionRow(
                    title = "$mbps Mbps",
                    subtitle = desc,
                    selected = config.bitrateMbps == mbps,
                    onClick = {
                        onConfigChange(config.copy(bitrateMbps = mbps))
                    }
                )
            }
        }

        // Section: Audio
        SettingsSection(title = "INTERNAL AUDIO") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Capture Internal Audio",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OneUITextPrimary
                    )
                    Text(
                        text = "Records game and media playback audio via Android AudioPlaybackCapture (API 29+).",
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = config.recordInternalAudio,
                    onCheckedChange = { onConfigChange(config.copy(recordInternalAudio = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OneUIRecordRed,
                        uncheckedThumbColor = OneUITextSecondary,
                        uncheckedTrackColor = OneUISurfaceVariant
                    ),
                    modifier = Modifier.testTag("internal_audio_switch")
                )
            }

            // Note about microphone and DRM
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OneUISurfaceVariant)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.MicOff,
                        contentDescription = null,
                        tint = OneUITextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Microphone input is disabled for clean sound and privacy. Note that apps with DRM or restricted playback policies cannot be recorded per Android system policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextSecondary
                    )
                }
            }
        }

        // Section: Countdown
        SettingsSection(title = "PRE-RECORDING COUNTDOWN") {
            listOf(
                0 to "Off (Starts recording immediately)",
                3 to "3 Seconds (Gives you time to switch apps)"
            ).forEach { (seconds, desc) ->
                SelectableOptionRow(
                    title = if (seconds == 0) "Immediate" else "$seconds Seconds",
                    subtitle = desc,
                    selected = config.countdownSeconds == seconds,
                    onClick = {
                        onConfigChange(config.copy(countdownSeconds = seconds))
                    }
                )
            }
        }

        // Section: Storage Info
        SettingsSection(title = "STORAGE LOCATION") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = OneUIRecordRed)
                Column {
                    Text(
                        text = "Movies/LiteRec",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OneUITextPrimary
                    )
                    Text(
                        text = "Stored directly in device standard MediaStore. Immediately accessible in Samsung Gallery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            ),
            color = OneUITextMuted,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OneUISurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OneUIBorder)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SelectableOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) OneUIRecordRed else OneUITextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OneUITextSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Radio check dot
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .border(2.dp, if (selected) OneUIRecordRed else OneUIBorder, androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) OneUIRecordRed else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}
