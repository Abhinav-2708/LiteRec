package com.example.recorder

import android.content.ContentValues
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.example.model.RecordingConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecorderEngine(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val config: RecordingConfig,
    private val onAudioStatusChanged: (isActive: Boolean, errorMessage: String?) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "ScreenRecorderEngine"
    }

    private var pfd: ParcelFileDescriptor? = null
    private var outputUri: Uri? = null
    private var fileName: String = ""

    private var muxerManager: MediaMuxerManager? = null
    private var videoEncoder: ScreenVideoEncoder? = null
    private var audioEncoder: InternalAudioEncoder? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var recordingWidth: Int = 0
    private var recordingHeight: Int = 0
    private var sessionStartTimeMs: Long = 0L

    val currentWidth: Int get() = recordingWidth
    val currentHeight: Int get() = recordingHeight

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            Log.d(TAG, "MediaProjection revoked/stopped by system")
            onError("Screen capture permission was revoked by system")
        }
    }

    fun start(): Uri? {
        try {
            mediaProjection.registerCallback(projectionCallback, null)

            // Calculate video dimensions keeping aspect ratio and even multiples
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)

            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val screenDensityDpi = metrics.densityDpi

            val (w, h) = config.calculateDimensions(screenWidth, screenHeight)
            recordingWidth = w
            recordingHeight = h

            Log.d(TAG, "Starting recorder: Display=${screenWidth}x${screenHeight}, Output=${w}x${h}, DPI=$screenDensityDpi")

            // Create MediaStore output file under Movies/LiteRec
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            fileName = "LiteRec_${dateFormat.format(Date())}.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LiteRec")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Failed to create MediaStore entry for video recording")
            outputUri = uri

            val descriptor = context.contentResolver.openFileDescriptor(uri, "rw")
                ?: throw IllegalStateException("Failed to open ParcelFileDescriptor for $uri")
            pfd = descriptor

            // Setup audio encoder first if requested
            val wantsAudio = config.recordInternalAudio
            var audioPrepared = false
            var aEncoder: InternalAudioEncoder? = null

            // Initialize Muxer Manager tentatively
            val muxer = MediaMuxerManager(descriptor.fileDescriptor, hasAudioTrack = wantsAudio)
            muxerManager = muxer

            if (wantsAudio) {
                aEncoder = InternalAudioEncoder(
                    mediaProjection = mediaProjection,
                    muxerManager = muxer,
                    onAudioStatusChanged = onAudioStatusChanged
                )
                audioPrepared = aEncoder.prepare()
                if (audioPrepared) {
                    audioEncoder = aEncoder
                } else {
                    Log.w(TAG, "Audio capture could not be initialized; proceeding with video-only recording")
                    muxer.setAudioExpected(false)
                }
            } else {
                muxer.setAudioExpected(false)
                onAudioStatusChanged(false, "Audio recording disabled in settings")
            }

            // Initialize Video Encoder
            val vEncoder = ScreenVideoEncoder(
                width = recordingWidth,
                height = recordingHeight,
                fps = config.fps,
                bitrateBps = config.bitrateBps,
                muxerManager = muxer,
                onError = onError
            )
            val inputSurface = vEncoder.prepare()
            videoEncoder = vEncoder

            // Create Virtual Display mirroring screen into hardware encoder surface
            val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "LiteRec-VirtualDisplay",
                recordingWidth,
                recordingHeight,
                screenDensityDpi,
                flags,
                inputSurface,
                null,
                null
            )

            sessionStartTimeMs = SystemClock.elapsedRealtime()

            // Start draining loops
            vEncoder.start()
            if (audioPrepared) {
                audioEncoder?.start()
            }

            Log.d(TAG, "ScreenRecorderEngine successfully started: $outputUri")
            return outputUri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ScreenRecorderEngine", e)
            onError("Recording initialization failed: ${e.localizedMessage}")
            stop()
            return null
        }
    }

    fun stop(): Pair<Uri?, String?> {
        Log.d(TAG, "Stopping ScreenRecorderEngine...")

        val actualDurationMs = if (sessionStartTimeMs > 0) {
            SystemClock.elapsedRealtime() - sessionStartTimeMs
        } else 0L

        try {
            mediaProjection.unregisterCallback(projectionCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister MediaProjection callback", e)
        }

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release VirtualDisplay", e)
        }
        virtualDisplay = null

        audioEncoder?.stop()
        audioEncoder = null

        videoEncoder?.stop()
        videoEncoder = null

        val muxerSuccess = muxerManager?.stopAndRelease() ?: false
        muxerManager = null

        try {
            pfd?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close ParcelFileDescriptor", e)
        }
        pfd = null

        val uri = outputUri
        if (uri != null) {
            try {
                if (muxerSuccess) {
                    val updateValues = ContentValues().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.IS_PENDING, 0)
                        }
                        if (actualDurationMs > 0) {
                            put(MediaStore.Video.Media.DURATION, actualDurationMs)
                        }
                    }
                    context.contentResolver.update(uri, updateValues, null, null)
                    Log.d(TAG, "MediaStore recording marked complete ($actualDurationMs ms): $uri")
                } else {
                    // Recording was empty or failed; clean up incomplete entry
                    context.contentResolver.delete(uri, null, null)
                    Log.w(TAG, "Removed incomplete MediaStore entry: $uri")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error finalizing MediaStore entry", e)
            }
        }

        try {
            mediaProjection.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop MediaProjection", e)
        }

        return Pair(if (muxerSuccess) uri else null, fileName)
    }
}
