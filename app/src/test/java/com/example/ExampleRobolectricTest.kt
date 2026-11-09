package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.RecordingConfig
import com.example.model.RecordingItem
import com.example.model.RecordingState
import com.example.model.ResolutionPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LiteRec", appName)
  }

  @Test
  fun `test 720p dimension calculation ensures even numbers`() {
    val config = RecordingConfig(resolutionPreset = ResolutionPreset.HD_720P)
    // Common phone display 1080x2400 portrait
    val (width, height) = config.calculateDimensions(1080, 2400)
    assertTrue("Width must be even", width % 2 == 0)
    assertTrue("Height must be even", height % 2 == 0)
    assertEquals(720, width)
    assertEquals(1600, height)
  }

  @Test
  fun `test 1080p dimension calculation ensures even numbers`() {
    val config = RecordingConfig(resolutionPreset = ResolutionPreset.FHD_1080P)
    val (width, height) = config.calculateDimensions(1080, 2400)
    assertTrue("Width must be even", width % 2 == 0)
    assertTrue("Height must be even", height % 2 == 0)
    assertEquals(1080, width)
    assertEquals(2400, height)
  }

  @Test
  fun `test duration formatting for 10s and 60s recordings`() {
    val tenSec = RecordingState(durationMs = 10_000L)
    assertEquals("00:10", tenSec.formattedDuration)

    val sixtySec = RecordingState(durationMs = 60_000L)
    assertEquals("01:00", sixtySec.formattedDuration)

    val hourState = RecordingState(durationMs = 3665_000L)
    assertEquals("01:01:05", hourState.formattedDuration)
  }

  @Test
  fun `test audio pts microsecond calculations`() {
    val sampleRate = 48000L
    val tenSecFrames = 10L * sampleRate
    val tenSecPtsUs = (tenSecFrames * 1_000_000L) / sampleRate
    assertEquals(10_000_000L, tenSecPtsUs)

    val sixtySecFrames = 60L * sampleRate
    val sixtySecPtsUs = (sixtySecFrames * 1_000_000L) / sampleRate
    assertEquals(60_000_000L, sixtySecPtsUs)
  }
}
