package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RecordingItem
import com.example.ui.components.OneUIHeader
import com.example.ui.theme.OneUIBackground
import com.example.ui.theme.OneUIBorder
import com.example.ui.theme.OneUIRecordRed
import com.example.ui.theme.OneUISurface
import com.example.ui.theme.OneUISurfaceVariant
import com.example.ui.theme.OneUITextMuted
import com.example.ui.theme.OneUITextPrimary
import com.example.ui.theme.OneUITextSecondary

@Composable
fun RecordingsScreen(
    recordings: List<RecordingItem>,
    isRecording: Boolean,
    onPlay: (RecordingItem) -> Unit,
    onShare: (RecordingItem) -> Unit,
    onDelete: (RecordingItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var itemToDelete by remember { mutableStateOf<RecordingItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBackground)
    ) {
        OneUIHeader(
            title = "Recordings",
            subtitle = "Saved in Movies/LiteRec",
            isRecording = isRecording
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${recordings.size} RECORDING${if (recordings.size == 1) "" else "S"}",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = OneUITextMuted
            )
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("refresh_recordings_button")
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh recordings",
                    tint = OneUITextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(OneUISurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = OneUITextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "No recordings yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OneUITextPrimary
                    )
                    Text(
                        text = "Videos you record will appear here and in your phone's Gallery under Movies/LiteRec.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OneUITextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordings, key = { it.id }) { item ->
                    RecordingCard(
                        item = item,
                        onPlay = { onPlay(item) },
                        onShare = { onShare(item) },
                        onDeleteClick = { itemToDelete = item }
                    )
                }
            }
        }
    }

    itemToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Delete Recording?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OneUITextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${target.name}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OneUITextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(target)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OneUIRecordRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = OneUITextSecondary)
                }
            },
            containerColor = OneUISurface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun RecordingCard(
    item: RecordingItem,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recording_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = OneUISurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OneUIBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play thumbnail button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OneUISurfaceVariant)
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Info column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onPlay)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = OneUITextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.formattedDuration} • ${item.formattedSize}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OneUITextSecondary
                )
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = OneUITextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = OneUIRecordRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
