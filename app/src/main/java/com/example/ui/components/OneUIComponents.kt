package com.example.ui.components

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.RecordingItem
import com.example.service.LiteRecTileService
import com.example.ui.theme.OneUIBackground
import com.example.ui.theme.OneUIBorder
import com.example.ui.theme.OneUIIdleGlow
import com.example.ui.theme.OneUIIdleGreen
import com.example.ui.theme.OneUIRecordGlow
import com.example.ui.theme.OneUIRecordRed
import com.example.ui.theme.OneUISurface
import com.example.ui.theme.OneUISurfaceVariant
import com.example.ui.theme.OneUITextMuted
import com.example.ui.theme.OneUITextPrimary
import com.example.ui.theme.OneUITextSecondary

@Composable
fun OneUIHeader(
    title: String,
    subtitle: String,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = OneUITextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OneUITextSecondary
                )
            }

            // Status Badge
            val badgeBg = if (isRecording) OneUIRecordGlow else OneUIIdleGlow
            val badgeBorder = if (isRecording) OneUIRecordRed else OneUIIdleGreen
            val badgeText = if (isRecording) "RECORDING" else "IDLE"
            val badgeColor = if (isRecording) OneUIRecordRed else OneUIIdleGreen

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeBg)
                    .border(1.dp, badgeBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("status_badge"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
fun HeroRecordButton(
    isRecording: Boolean,
    countdown: Int?,
    durationText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(164.dp)
        ) {
            // Pulsing outer aura ring when recording
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(OneUIRecordGlow)
                )
            }

            // Main tactile touch button
            val btnColor = if (isRecording) OneUIRecordRed else OneUISurfaceVariant
            val borderColor = if (isRecording) Color.White.copy(alpha = 0.4f) else OneUIBorder

            Box(
                modifier = Modifier
                    .size(136.dp)
                    .clip(CircleShape)
                    .background(btnColor)
                    .border(2.dp, borderColor, CircleShape)
                    .clickable(onClick = onClick)
                    .testTag("hero_record_button"),
                contentAlignment = Alignment.Center
            ) {
                if (countdown != null) {
                    Text(
                        text = "$countdown",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                        color = OneUIRecordRed
                    )
                } else if (isRecording) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Stop square icon
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "STOP",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Record circle dot
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(OneUIRecordRed)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "START",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = OneUITextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Timer or Prompt
        if (isRecording) {
            Text(
                text = durationText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = OneUITextPrimary,
                modifier = Modifier.testTag("recording_timer_text")
            )
            Text(
                text = "Tap to finish & save recording",
                style = MaterialTheme.typography.bodySmall,
                color = OneUITextSecondary
            )
        } else {
            Text(
                text = "Tap button or use Quick Settings tile",
                style = MaterialTheme.typography.bodyMedium,
                color = OneUITextSecondary
            )
        }
    }
}

@Composable
fun TileGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Quick Settings Tile Guide",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = OneUITextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Control LiteRec instantly from your notification shade without opening the app:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OneUITextSecondary
                )

                StepItem(step = "1", text = "Swipe down twice from top of screen to expand Quick Settings.")
                StepItem(step = "2", text = "Tap the 3 dots or pencil icon (Edit buttons on Samsung One UI).")
                StepItem(step = "3", text = "Locate the 'LiteRec' tile and drag it to your active tiles.")
                StepItem(step = "4", text = "Done! Tap the tile anytime to start or stop screen recording.")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            try {
                                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                                val componentName = ComponentName(context, LiteRecTileService::class.java)
                                statusBarManager?.requestAddTileService(
                                    componentName,
                                    "LiteRec",
                                    android.graphics.drawable.Icon.createWithResource(context, com.example.R.drawable.ic_qs_record),
                                    context.mainExecutor
                                ) { }
                            } catch (ignored: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OneUISurfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_add_tile_button")
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = OneUIRecordRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Tile Automatically (Android 13+)", color = OneUITextPrimary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OneUIRecordRed)
            ) {
                Text("Got it", color = Color.White)
            }
        },
        containerColor = OneUISurface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun StepItem(step: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(OneUISurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = OneUITextPrimary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = OneUITextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun VideoPlayerDialog(
    item: RecordingItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OneUITextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = OneUITextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Video View Player with dynamic aspect ratio based on orientation
                val videoAspect = if (item.width > 0 && item.height > 0) {
                    (item.width.toFloat() / item.height.toFloat()).coerceIn(0.55f, 1.8f)
                } else (16f / 9f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 320.dp)
                        .aspectRatio(videoAspect, matchHeightConstraintsFirst = false)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setVideoURI(item.uri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Duration: ${item.formattedDuration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextSecondary
                    )
                    Text(
                        text = "Size: ${item.formattedSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_share_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OneUIBorder)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = OneUITextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = OneUITextPrimary)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_delete_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OneUIRecordRed.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = OneUIRecordRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", color = OneUIRecordRed)
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = OneUISurface,
        shape = RoundedCornerShape(24.dp)
    )
}
