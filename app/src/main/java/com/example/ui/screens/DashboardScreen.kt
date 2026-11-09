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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordingItem
import com.example.model.RecordingState
import com.example.ui.components.HeroRecordButton
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
import com.example.ui.theme.OneUIWarningAmber

@Composable
fun DashboardScreen(
    state: RecordingState,
    countdown: Int?,
    latestRecording: RecordingItem?,
    onRecordToggle: () -> Unit,
    onOpenTileGuide: () -> Unit,
    onPlayRecording: (RecordingItem) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        OneUIHeader(
            title = "LiteRec",
            subtitle = "Low-Overhead Screen Capture",
            isRecording = state.isRecording
        )

        // Error Banner
        if (state.errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = OneUIRecordRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OneUIRecordRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = OneUIRecordRed)
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = OneUIRecordRed,
                        modifier = Modifier
                            .clickable(onClick = onDismissError)
                            .padding(4.dp)
                    )
                }
            }
        }

        // Audio Warning Banner (if internal audio blocked/unavailable)
        if (state.audioErrorMessage != null && state.isRecording) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = OneUIWarningAmber.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OneUIWarningAmber.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = OneUIWarningAmber)
                    Text(
                        text = state.audioErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Hero Record Control
        HeroRecordButton(
            isRecording = state.isRecording,
            countdown = countdown,
            durationText = state.formattedDuration,
            onClick = onRecordToggle
        )

        // Config summary card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clickable(onClick = onNavigateToSettings)
                .testTag("config_summary_card"),
            colors = CardDefaults.cardColors(containerColor = OneUISurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OneUIBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE PRESET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = OneUITextMuted
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Configure",
                            style = MaterialTheme.typography.labelSmall,
                            color = OneUIRecordRed
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = OneUIRecordRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfigPill(
                        icon = Icons.Default.HighQuality,
                        label = state.config.resolutionPreset.name.replace("_", " "),
                        modifier = Modifier.weight(1f)
                    )
                    ConfigPill(
                        icon = Icons.Default.Speed,
                        label = "${state.config.fps} FPS",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConfigPill(
                        icon = Icons.Default.Videocam,
                        label = "${state.config.bitrateMbps} Mbps",
                        modifier = Modifier.weight(1f)
                    )
                    ConfigPill(
                        icon = Icons.Default.Audiotrack,
                        label = if (state.config.recordInternalAudio) "Playback Audio" else "Audio Muted",
                        tint = if (state.config.recordInternalAudio) OneUIIdleGreen else OneUITextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Settings Tile Shortcut Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clickable(onClick = onOpenTileGuide)
                .testTag("tile_guide_card"),
            colors = CardDefaults.cardColors(containerColor = OneUISurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OneUIBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(OneUISurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        tint = OneUIRecordRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Samsung Quick Settings Tile",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = OneUITextPrimary
                    )
                    Text(
                        text = "Record from notification shade anytime",
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextSecondary
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = OneUITextSecondary
                )
            }
        }

        // Latest Recording preview
        if (latestRecording != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = OneUISurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OneUIBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OneUISurfaceVariant)
                            .clickable { onPlayRecording(latestRecording) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Latest Recording",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = OneUIIdleGreen
                        )
                        Text(
                            text = latestRecording.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = OneUITextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "${latestRecording.formattedDuration} • ${latestRecording.formattedSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OneUITextSecondary
                        )
                    }
                }
            }
        }

        // Generous bottom clearance to prevent overlap with navigation bar
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun ConfigPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    tint: Color = OneUITextPrimary
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(OneUISurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = OneUITextPrimary,
            maxLines = 1
        )
    }
}
