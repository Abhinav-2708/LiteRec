package com.example.recorder

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class InternalAudioEncoder(
    private val mediaProjection: MediaProjection,
    private val muxerManager: MediaMuxerManager,
    private val onAudioStatusChanged: (isActive: Boolean, errorMessage: String?) -> Unit
) {
    companion object {
        private const val TAG = "InternalAudioEncoder"
        private const val CHANNEL_COUNT = 2
        private const val BIT_RATE = 128_000
        private const val TIMEOUT_USEC = 10_000L
    }

    private var sampleRate = 48000 // Android 10+ native playback capture standard
    private var audioRecord: AudioRecord? = null
    private var codec: MediaCodec? = null
    private var audioTrackIndex = -1
    private val isRunning = AtomicBoolean(false)
    private var audioThread: Thread? = null

    private var totalFramesRead = 0L
    private var lastAudioPtsUs = -1L

    @SuppressLint("MissingPermission")
    fun prepare(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onAudioStatusChanged(false, "Internal audio capture requires Android 10 (API 29)+")
            return false
        }

        try {
            // Select optimal hardware sample rate (prefer 48000 Hz, fallback to 44100 Hz)
            val candidateRates = intArrayOf(48000, 44100)
            var selectedRecord: AudioRecord? = null
            var selectedRate = 48000
            var finalBufferSize = 8192

            val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()

            for (rate in candidateRates) {
                val minSize = AudioRecord.getMinBufferSize(
                    rate,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minSize > 0) {
                    val bufferSize = (minSize * 2).coerceAtLeast(8192)
                    try {
                        val record = AudioRecord.Builder()
                            .setAudioPlaybackCaptureConfig(playbackConfig)
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(rate)
                                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                                    .build()
                            )
                            .setBufferSizeInBytes(bufferSize)
                            .build()

                        if (record.state == AudioRecord.STATE_INITIALIZED) {
                            selectedRecord = record
                            selectedRate = rate
                            finalBufferSize = bufferSize
                            break
                        } else {
                            record.release()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to initialize AudioRecord at $rate Hz: ${e.message}")
                    }
                }
            }

            if (selectedRecord == null) {
                Log.e(TAG, "AudioRecord failed to initialize with AudioPlaybackCaptureConfiguration")
                onAudioStatusChanged(false, "Internal audio capture initialization failed on device")
                return false
            }

            sampleRate = selectedRate
            audioRecord = selectedRecord

            // Configure AAC MediaCodec encoder
            val aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, CHANNEL_COUNT).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, finalBufferSize)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            codec = encoder

            totalFramesRead = 0L
            lastAudioPtsUs = -1L

            // Prime encoder with initial silence frame so audio track format is produced immediately
            primeEncoderWithSilence(encoder, sampleRate)

            Log.d(TAG, "InternalAudioEncoder prepared at ${sampleRate}Hz stereo AAC")
            onAudioStatusChanged(true, null)
            return true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException preparing AudioPlaybackCapture: ${e.message}")
            onAudioStatusChanged(false, "Audio permission denied or restricted by system")
            cleanup()
            return false
        } catch (e: Exception) {
            Log.w(TAG, "Internal audio capture setup failed: ${e.localizedMessage}")
            onAudioStatusChanged(false, "Audio capture unavailable: ${e.localizedMessage}")
            cleanup()
            return false
        }
    }

    private fun primeEncoderWithSilence(encoder: MediaCodec, rate: Int) {
        try {
            val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
            if (inputIndex >= 0) {
                val inputBuffer = encoder.getInputBuffer(inputIndex)
                if (inputBuffer != null) {
                    val silenceBytes = 2048
                    val silence = ByteArray(silenceBytes) // zeros = silence
                    inputBuffer.clear()
                    inputBuffer.put(silence)
                    encoder.queueInputBuffer(inputIndex, 0, silenceBytes, 0L, 0)
                    totalFramesRead += (silenceBytes / (CHANNEL_COUNT * 2))
                    lastAudioPtsUs = 0L

                    // Drain output buffer right away to obtain format
                    val bufferInfo = MediaCodec.BufferInfo()
                    drainEncoder(encoder, bufferInfo)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error priming AAC encoder with silence", e)
        }
    }

    fun start() {
        val record = audioRecord ?: return
        val encoder = codec ?: return

        isRunning.set(true)
        audioThread = Thread({
            try {
                record.startRecording()
                runAudioLoop(record, encoder)
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "Exception in audio recording loop", e)
                    onAudioStatusChanged(false, "Playback audio interrupted or app policy restricted")
                }
            }
        }, "InternalAudioEncoder-Loop").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun runAudioLoop(record: AudioRecord, encoder: MediaCodec) {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val readBufferSize = minBufferSize.coerceAtLeast(4096)
        val pcmBuffer = ByteArray(readBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        while (isRunning.get()) {
            val bytesRead = record.read(pcmBuffer, 0, pcmBuffer.size)
            if (bytesRead > 0) {
                val inputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
                if (inputIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        inputBuffer.put(pcmBuffer, 0, bytesRead)

                        val framesInChunk = bytesRead / (CHANNEL_COUNT * 2)
                        var ptsUs = (totalFramesRead * 1_000_000L) / sampleRate
                        if (ptsUs <= lastAudioPtsUs) {
                            ptsUs = lastAudioPtsUs + 250L // Ensure monotonic advance
                        }
                        lastAudioPtsUs = ptsUs
                        totalFramesRead += framesInChunk

                        encoder.queueInputBuffer(inputIndex, 0, bytesRead, ptsUs, 0)
                    }
                }
            } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION || bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                // If playback capture encounters a device audio policy glitch, avoid busy looping
                try {
                    Thread.sleep(15)
                } catch (ignored: InterruptedException) {
                    break
                }
            }

            // Continuously drain AAC encoder output
            drainEncoder(encoder, bufferInfo)
        }

        // Finalize stream with end-of-stream flag
        try {
            val eosIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
            if (eosIndex >= 0) {
                val finalPtsUs = if (lastAudioPtsUs > 0) lastAudioPtsUs + 1000L else 0L
                encoder.queueInputBuffer(eosIndex, 0, 0, finalPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            drainEncoder(encoder, bufferInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Error finalizing audio encoder EOS", e)
        }
    }

    private fun drainEncoder(encoder: MediaCodec, bufferInfo: MediaCodec.BufferInfo) {
        while (true) {
            val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                Log.d(TAG, "Audio output format changed: $newFormat")
                audioTrackIndex = muxerManager.addAudioTrack(newFormat)
            } else if (outIndex >= 0) {
                val outBuffer = encoder.getOutputBuffer(outIndex)
                if (outBuffer != null && bufferInfo.size > 0 && audioTrackIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        muxerManager.writeSampleData(false, audioTrackIndex, outBuffer, bufferInfo)
                    }
                }
                encoder.releaseOutputBuffer(outIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            } else {
                break
            }
        }
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) return

        audioThread?.interrupt()
        try {
            audioThread?.join(600)
        } catch (ignored: InterruptedException) {}
        audioThread = null

        cleanup()
        Log.d(TAG, "Internal audio encoder stopped and resources released.")
    }

    private fun cleanup() {
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop AudioRecord", e)
        }
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release AudioRecord", e)
        }
        audioRecord = null

        try {
            codec?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop audio codec", e)
        }
        try {
            codec?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release audio codec", e)
        }
        codec = null
    }
}
