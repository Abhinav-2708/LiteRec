# LiteRec — Lightweight Native Android Screen Recorder

LiteRec is a high-performance, low-overhead native Android screen and internal audio recording application. Built with Kotlin and modern Jetpack Compose, it features a Samsung One UI-inspired dark charcoal interface, zero-copy hardware video encoding, Android 10+ playback audio capture, and a native Android Quick Settings tile.

---

## 1. Key Architectural Highlights

- **Hardware-Accelerated Zero-Copy Video Pipeline:**
  - Uses `MediaProjection.createVirtualDisplay()` targeting the input `Surface` of a hardware `MediaCodec` H.264 (`video/avc`) encoder (`COLOR_FormatSurface`).
  - Screen pixels are rendered directly by the Android GPU into the hardware encoder surface — eliminating user-space frame copying, memory allocations, and CPU overhead.
  - Video dimensions are scaled keeping the screen aspect ratio and aligned to even multiples of 16 for optimal hardware encoder compatibility.

- **Internal Playback Audio Capture (Android 10+ / API 29+):**
  - Utilizes `AudioPlaybackCaptureConfiguration` with `USAGE_MEDIA`, `USAGE_GAME`, and `USAGE_UNKNOWN` matching rules.
  - Records PCM stereo at 44.1 kHz via `AudioRecord` and encodes to AAC (`audio/mp4a-latm`) at 128 kbps using `MediaCodec`.
  - Microphone input is intentionally disabled to avoid room echo, noise, and ensure complete user privacy.
  - Gracefully falls back to video-only recording with an informational warning if apps or system policies disallow audio capture (e.g. DRM-protected content).

- **Synchronized MP4 Muxing:**
  - Thread-safe `MediaMuxerManager` orchestrates video and audio tracks, buffers pre-roll frames until all active tracks are registered, and writes monotonic presentation timestamps (`ptsUs`).
  - Safely stops and finalizes MP4 files without crashing on short or interrupted recordings.

- **Scoped Storage & MediaStore (`Movies/LiteRec`):**
  - Uses standard Android `MediaStore.Video.Media` with `RELATIVE_PATH = "Movies/LiteRec"`.
  - Marks files as `IS_PENDING = 1` during recording and `IS_PENDING = 0` upon completion.
  - No broad storage permissions required; recordings appear immediately in Samsung Gallery and Google Photos.

- **Samsung Quick Settings Tile (`TileService`):**
  - Implements `LiteRecTileService` (`android.service.quicksettings.TileService`).
  - Displays dynamic state: `STATE_INACTIVE` ("LiteRec", "Tap to record") and `STATE_ACTIVE` ("Recording", live duration subtitle).
  - Tapping when active immediately stops recording.
  - Tapping when idle invokes `startActivityAndCollapse` to trigger the system screen capture consent dialog.

- **Persistent Recording Notification:**
  - Registered as `FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` (Android 14+ / API 34+ compliant).
  - Shows live elapsed duration ("01:24 • 30 FPS • Audio ON"), direct "Stop" action button, and tap-to-open pending intent.

---

## 2. Samsung Galaxy Build & Installation Instructions

### Prerequisites
- Samsung Galaxy device running Android 10 (One UI 2.0) or newer (One UI 3.x, 4.x, 5.x, 6.x, 7.x).
- Android Studio Ladybug / Meerkat or Gradle command line.
- USB Debugging enabled on your Samsung Galaxy:
  1. Go to **Settings > About phone > Software information**.
  2. Tap **Build number** 7 times until Developer mode is enabled.
  3. Go to **Settings > Developer options** and enable **USB debugging**.

### Building the APK
To compile the debug APK:
```bash
gradle :app:assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

To install directly onto your connected Samsung device via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Generating a Signed Release APK
1. In Android Studio, go to **Build > Generate Signed Bundle / APK...**.
2. Select **APK** and click **Next**.
3. Choose your release keystore, alias, and passwords.
4. Select build variant **release**, check **V1 (Jar Signature)** and **V2 (Full APK Signature)**, and click **Finish**.
Alternatively via Gradle:
```bash
STORE_PASSWORD="your_password" KEY_PASSWORD="your_password" KEYSTORE_PATH="/path/to/keystore.jks" gradle :app:assembleRelease
```

---

## 3. Adding the Quick Settings Tile on Samsung One UI

1. Swipe down twice from the top of your Samsung Galaxy screen to open the full Quick Settings panel.
2. Tap the **Pencil icon** (One UI 6+) or the **3 dots menu** in the top right, then select **Edit buttons**.
3. Under the list of available tiles at the top/bottom, locate **LiteRec**.
4. Drag and drop the **LiteRec** tile into your active quick panel.
5. Tap **Done**.
6. Now you can start and stop screen recordings anytime directly from your notification shade.

---

## 4. Test Checklist (Android 10 through Android 15/16)

| Test Case | Expected Result | Verified |
|---|---|:---:|
| **Launch & One UI Theme** | App launches with dark charcoal `#111215` styling, status pill shows "READY" | Verified |
| **System Capture Consent** | Tapping "START" launches `MediaProjection` system prompt | Verified |
| **Permission Rejection** | Cancelling system prompt updates status to idle with friendly message | Verified |
| **Active Screen Recording** | Status turns red "RECORDING", elapsed timer ticks, persistent notification shows | Verified |
| **Internal Audio Capture** | Compatible game/music audio is captured into MP4 AAC track | Verified |
| **DRM / Silenced Audio** | Restricted audio fails gracefully; video recording continues smoothly | Verified |
| **Notification Stop Action** | Tapping "Stop" in notification immediately finalizes MP4 and clears foreground service | Verified |
| **Quick Settings Tile Toggle** | Tapping tile stops active recording; tapping idle collapses shade and initiates capture | Verified |
| **MediaStore Storage** | Output saved to `Movies/LiteRec/LiteRec_YYYYMMDD_HHMMSS.mp4` and indexed in Gallery | Verified |
| **Library Management** | In-app playback, system share chooser, and deletion with confirmation work properly | Verified |
| **Orientation Adaptation** | Portrait and landscape dimensions are computed and aligned to multiples of 16 | Verified |
| **Android 14+ FGS Compliance** | `FOREGROUND_SERVICE_MEDIA_PROJECTION` registered before MediaProjection acquisition | Verified |
