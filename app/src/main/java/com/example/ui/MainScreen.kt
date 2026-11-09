package com.example.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.RecordingsRepository
import com.example.ui.components.TileGuideDialog
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.OneUIBackground
import com.example.ui.theme.OneUIBorder
import com.example.ui.theme.OneUIRecordRed
import com.example.ui.theme.OneUISurface
import com.example.ui.theme.OneUISurfaceVariant
import com.example.ui.theme.OneUITextPrimary
import com.example.ui.theme.OneUITextSecondary

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestStartRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordingState by viewModel.recordingState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val showTileGuide by viewModel.showTileGuide.collectAsState()
    val previewVideo by viewModel.previewVideo.collectAsState()

    val repository = RecordingsRepository(context)

    // Handle back button to pop dialogs or return to dashboard tab
    BackHandler(enabled = previewVideo != null || showTileGuide || selectedTab != AppTab.DASHBOARD) {
        when {
            previewVideo != null -> viewModel.setPreviewVideo(null)
            showTileGuide -> viewModel.setShowTileGuide(false)
            selectedTab != AppTab.DASHBOARD -> viewModel.selectTab(AppTab.DASHBOARD)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(OneUIBackground),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = OneUISurface,
                contentColor = OneUITextPrimary,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.DASHBOARD,
                    onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == AppTab.DASHBOARD) Icons.Filled.FiberManualRecord else Icons.Outlined.FiberManualRecord,
                            contentDescription = "Record"
                        )
                    },
                    label = { Text("Record") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OneUIRecordRed,
                        selectedTextColor = OneUIRecordRed,
                        indicatorColor = OneUISurfaceVariant,
                        unselectedIconColor = OneUITextSecondary,
                        unselectedTextColor = OneUITextSecondary
                    ),
                    modifier = Modifier.testTag("nav_record_tab")
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.RECORDINGS,
                    onClick = {
                        viewModel.selectTab(AppTab.RECORDINGS)
                        viewModel.loadRecordings()
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == AppTab.RECORDINGS) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                            contentDescription = "Recordings"
                        )
                    },
                    label = { Text("Library") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OneUIRecordRed,
                        selectedTextColor = OneUIRecordRed,
                        indicatorColor = OneUISurfaceVariant,
                        unselectedIconColor = OneUITextSecondary,
                        unselectedTextColor = OneUITextSecondary
                    ),
                    modifier = Modifier.testTag("nav_library_tab")
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.SETTINGS,
                    onClick = { viewModel.selectTab(AppTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = OneUIRecordRed,
                        selectedTextColor = OneUIRecordRed,
                        indicatorColor = OneUISurfaceVariant,
                        unselectedIconColor = OneUITextSecondary,
                        unselectedTextColor = OneUITextSecondary
                    ),
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.DASHBOARD -> {
                    DashboardScreen(
                        state = recordingState,
                        countdown = countdown,
                        latestRecording = recordings.firstOrNull(),
                        onRecordToggle = {
                            triggerHaptic(context)
                            if (recordingState.isRecording) {
                                viewModel.stopRecording()
                            } else {
                                if (countdown != null) {
                                    viewModel.cancelCountdown()
                                } else {
                                    viewModel.startCountdownThen {
                                        onRequestStartRecording()
                                    }
                                }
                            }
                        },
                        onOpenTileGuide = { viewModel.setShowTileGuide(true) },
                        onPlayRecording = { viewModel.setPreviewVideo(it) },
                        onNavigateToSettings = { viewModel.selectTab(AppTab.SETTINGS) },
                        onDismissError = { viewModel.clearError() }
                    )
                }
                AppTab.RECORDINGS -> {
                    RecordingsScreen(
                        recordings = recordings,
                        isRecording = recordingState.isRecording,
                        onPlay = { viewModel.setPreviewVideo(it) },
                        onShare = { item ->
                            val intent = repository.createShareIntent(item)
                            context.startActivity(android.content.Intent.createChooser(intent, "Share recording"))
                        },
                        onDelete = { viewModel.deleteRecording(it) },
                        onRefresh = { viewModel.loadRecordings() }
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsScreen(
                        config = recordingState.config,
                        isRecording = recordingState.isRecording,
                        onConfigChange = { viewModel.updateConfig(it) }
                    )
                }
            }
        }

        if (showTileGuide) {
            TileGuideDialog(onDismiss = { viewModel.setShowTileGuide(false) })
        }

        previewVideo?.let { item ->
            VideoPlayerDialog(
                item = item,
                onDismiss = { viewModel.setPreviewVideo(null) },
                onShare = {
                    val intent = repository.createShareIntent(item)
                    context.startActivity(android.content.Intent.createChooser(intent, "Share recording"))
                },
                onDelete = {
                    viewModel.deleteRecording(item)
                }
            )
        }
    }
}

private fun triggerHaptic(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(40)
        }
    } catch (ignored: Exception) {}
}
