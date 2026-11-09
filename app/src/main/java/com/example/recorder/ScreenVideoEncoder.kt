package com.example.recorder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean

class ScreenVideoEncoder(
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitrateBps: Int,
    private val muxerManager: MediaMuxerManager,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "ScreenVideoEncoder"
        private const val TIMEOUT_USEC = 10_000L
    }

    private var codec: MediaCodec? = null
    var inputSurface: Surface? = null
        private set

    private var videoTrackIndex = -1
    private val isRunning = AtomicBoolean(false)
    private var drainThread: Thread? = null

    // Presentation timestamp normalization to start at 0 microseconds
    private var firstFramePtsUs = -1L
    private var lastOutputPtsUs = -1L

    fun prepare(): Surface {
        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                // Low latency variable bitrate mode
                setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                setInteger(MediaFormat.KEY_PRIORITY, 0)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()
            codec = encoder

            firstFramePtsUs = -1L
            lastOutputPtsUs = -1L

            Log.d(TAG, "Video encoder configured: ${width}x${height} @ ${fps}fps, bitrate=$bitrateBps bps")
            return inputSurface ?: throw IllegalStateException("Input surface is null")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize video encoder", e)
            onError("Video encoder failure: ${e.localizedMessage}")
            throw e
        }
    }

    fun start() {
        val encoder = codec ?: return
        isRunning.set(true)
        drainThread = Thread({
            drainEncoder(encoder)
        }, "ScreenVideoEncoder-Drain").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun drainEncoder(encoder: MediaCodec) {
        val bufferInfo = MediaCodec.BufferInfo()
        while (isRunning.get()) {
            try {
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = encoder.outputFormat
                    Log.d(TAG, "Video output format changed: $newFormat")
                    videoTrackIndex = muxerManager.addVideoTrack(newFormat)
                } else if (outputBufferIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(outputBufferIndex)
                    if (encodedData != null && videoTrackIndex >= 0) {
                        if (bufferInfo.size > 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            // Normalize timestamp so video begins at 0 µs
                            val rawPts = bufferInfo.presentationTimeUs
                            if (firstFramePtsUs < 0) {
                                firstFramePtsUs = rawPts
                            }
                            var normalizedPts = rawPts - firstFramePtsUs
                            if (normalizedPts <= lastOutputPtsUs) {
                                normalizedPts = lastOutputPtsUs + 1000L // ensure at least 1ms progression
                            }
                            lastOutputPtsUs = normalizedPts
                            bufferInfo.presentationTimeUs = normalizedPts

                            muxerManager.writeSampleData(true, videoTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "End of stream reached in video encoder")
                        break
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "Error dequeuing output buffer in video encoder", e)
                }
                break
            }
        }
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) return

        try {
            codec?.signalEndOfInputStream()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to signal EOS on video input stream", e)
        }

        drainThread?.interrupt()
        try {
            drainThread?.join(600)
        } catch (ignored: InterruptedException) {}
        drainThread = null

        try {
            codec?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop video codec", e)
        }
        try {
            codec?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release video codec", e)
        }
        codec = null

        inputSurface?.release()
        inputSurface = null
        Log.d(TAG, "Video encoder stopped and resources released.")
    }
}
