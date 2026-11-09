package com.example.recorder

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MediaMuxerManager(
    private val fileDescriptor: FileDescriptor,
    private var hasAudioTrack: Boolean
) {
    companion object {
        private const val TAG = "MediaMuxerManager"
    }

    private var muxer: MediaMuxer? = null
    private val lock = ReentrantLock()

    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isStarted = false
    private var isStopped = false

    private val lastTrackPtsUs = mutableMapOf<Int, Long>()

    private data class PendingSample(
        val isVideo: Boolean,
        val buffer: ByteBuffer,
        val bufferInfo: MediaCodec.BufferInfo
    )

    private val pendingSamples = mutableListOf<PendingSample>()

    init {
        try {
            muxer = MediaMuxer(fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaMuxer with file descriptor", e)
        }
    }

    fun setAudioExpected(expected: Boolean) = lock.withLock {
        if (hasAudioTrack != expected) {
            hasAudioTrack = expected
            Log.d(TAG, "Audio track expected updated to: $expected")
            checkAndStartMuxer()
        }
    }

    fun addVideoTrack(format: MediaFormat): Int = lock.withLock {
        if (isStarted || muxer == null) return -1
        videoTrackIndex = muxer?.addTrack(format) ?: -1
        Log.d(TAG, "Added video track: index=$videoTrackIndex, format=$format")
        checkAndStartMuxer()
        return videoTrackIndex
    }

    fun addAudioTrack(format: MediaFormat): Int = lock.withLock {
        if (isStarted || muxer == null) return -1
        audioTrackIndex = muxer?.addTrack(format) ?: -1
        Log.d(TAG, "Added audio track: index=$audioTrackIndex, format=$format")
        checkAndStartMuxer()
        return audioTrackIndex
    }

    private fun checkAndStartMuxer() {
        if (isStarted || muxer == null) return

        val videoReady = videoTrackIndex >= 0
        val audioReady = !hasAudioTrack || (audioTrackIndex >= 0)

        if (videoReady && audioReady) {
            try {
                muxer?.start()
                isStarted = true
                Log.d(TAG, "MediaMuxer started successfully (videoTrack=$videoTrackIndex, audioTrack=$audioTrackIndex)")

                // Flush pending buffered samples with monotonic timestamp guarantee
                for (sample in pendingSamples) {
                    val track = if (sample.isVideo) videoTrackIndex else audioTrackIndex
                    if (track >= 0 && sample.bufferInfo.size > 0) {
                        writeSampleDataInternal(track, sample.buffer, sample.bufferInfo)
                    }
                }
                pendingSamples.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MediaMuxer", e)
            }
        }
    }

    fun writeSampleData(
        isVideo: Boolean,
        trackIndex: Int,
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) = lock.withLock {
        if (isStopped || muxer == null || (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            return
        }

        if (bufferInfo.size == 0) return

        if (!isStarted) {
            // Buffer bounded samples until tracks are ready (up to 120 samples)
            if (pendingSamples.size < 120) {
                try {
                    val copyBuffer = ByteBuffer.allocateDirect(bufferInfo.size)
                    val originalPos = byteBuffer.position()
                    val originalLimit = byteBuffer.limit()

                    byteBuffer.position(bufferInfo.offset)
                    byteBuffer.limit((bufferInfo.offset + bufferInfo.size).coerceAtMost(originalLimit))
                    copyBuffer.put(byteBuffer)

                    // Restore original buffer bounds
                    byteBuffer.position(originalPos)
                    byteBuffer.limit(originalLimit)
                    copyBuffer.flip()

                    val copyInfo = MediaCodec.BufferInfo().apply {
                        set(0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags)
                    }
                    pendingSamples.add(PendingSample(isVideo, copyBuffer, copyInfo))
                } catch (e: Exception) {
                    Log.w(TAG, "Error buffering pending sample before muxer start", e)
                }
            }
            return
        }

        val resolvedTrack = if (isVideo) videoTrackIndex else audioTrackIndex
        if (resolvedTrack >= 0) {
            writeSampleDataInternal(resolvedTrack, byteBuffer, bufferInfo)
        }
    }

    private fun writeSampleDataInternal(
        track: Int,
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        try {
            // Guarantee strictly monotonic presentation timestamps per track
            val lastPts = lastTrackPtsUs[track] ?: -1L
            var currentPts = bufferInfo.presentationTimeUs
            if (currentPts <= lastPts) {
                currentPts = lastPts + 1000L // enforce at least 1ms advance
                bufferInfo.presentationTimeUs = currentPts
            }
            lastTrackPtsUs[track] = currentPts

            muxer?.writeSampleData(track, byteBuffer, bufferInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing sample data to track $track (pts=${bufferInfo.presentationTimeUs})", e)
        }
    }

    fun stopAndRelease(): Boolean = lock.withLock {
        if (isStopped) return true
        isStopped = true

        var success = true
        try {
            if (isStarted) {
                muxer?.stop()
                Log.d(TAG, "MediaMuxer stopped cleanly.")
            } else {
                Log.w(TAG, "MediaMuxer was stopped before starting; no valid MP4 produced.")
                success = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MediaMuxer stop", e)
            success = false
        } finally {
            try {
                muxer?.release()
                Log.d(TAG, "MediaMuxer released.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception during MediaMuxer release", e)
            }
            muxer = null
            pendingSamples.clear()
            lastTrackPtsUs.clear()
        }
        return success
    }
}
