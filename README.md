# T2E Toolbox

[![Release](https://img.shields.io/badge/Release-v0.5.0-blue.svg)](releases/)
[![Platform](https://img.shields.io/badge/Platform-Android%2012%2B-green.svg)](https://developer.android.com)
[![Device](https://img.shields.io/badge/Device-Unihertz%20Titan%202%20Elite-orange.svg)](https://www.unihertz.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

A specialized companion utility for the **Unihertz Titan 2 Elite** (Android 15 / 16). T2E Toolbox enhances device usability with system display tuning, stock-style notification shade gestures, hardware side-key mode switching, and status bar indicators.

---

## Features

### 1. Status Bar Flashlight Indicator
- **Accidental Activation Prevention:** The Titan 2 Elite’s programmable hardware side key is frequently mapped to toggle the flashlight, often causing accidental pocket activations.
- **Dead-Center Icon:** Displays a clean, horizontal flashlight badge directly in the dead center of the status bar whenever the torch is active.
- **Transparent Touch Pass-Through:** Utilizes an accessibility overlay (`FLAG_NOT_TOUCHABLE`) ensuring all status bar swipes, shade pulls, and taps pass through without interference.
- **Global Torch Monitoring:** Tracks hardware state via `CameraManager.TorchCallback`, reacting instantly regardless of whether the torch was activated by the physical side key, Quick Settings, lockscreen, or third-party camera apps.
- **Customizable Appearance:** Choose between *Amber Glow*, *Bright Yellow*, and *Crisp White*, with an optional *Tap to Dismiss* touch shortcut.

### 2. Volume Mode Switcher
- **Hardware Key Shortcut:** Provides an invisible launcher entry (`VolumeModeActivity`) with zero window animations, specifically designed to be mapped to the Titan 2 Elite physical programmable side key (**Settings $\rightarrow$ Programmable Key $\rightarrow$ Launch app**).
- **Fast Cycle Sequence:** Instantly cycles between **Sound**, **Vibrate**, and **Silent** without opening a UI window.
- **Configurable Sequences:** Supports Sound $\rightarrow$ Vibrate $\rightarrow$ Silent (default), Sound $\leftrightarrow$ Vibrate, Sound $\leftrightarrow$ Silent, or Vibrate $\leftrightarrow$ Silent.
- **Tactile Haptic Cues:** Mode-specific vibration pulses (double pulse for vibrate, crisp tick for sound, subtle tap for silent) and an optional on-screen Toast.
- **1-Tap DND Grant:** Grant Do Not Disturb policy access seamlessly via Shizuku shell.

### 3. Unified Pull-Down
- **Stock-Style Status Bar Gestures:** Recreates stock Android status bar pull-down behavior on the square display.
- **Smart Shade Detection:** Inspects SystemUI window hierarchy to detect whether the shade is closed, showing notifications, or displaying Quick Settings tiles, cycling smoothly without timeouts:
  $$\text{Closed} \longrightarrow \text{Notifications} \longrightarrow \text{Quick Settings} \longrightarrow \text{Notifications}$$
- **Fallback 3-Second State Machine:** Alternate between notifications and Quick Settings when smart detection is off.
- **Haptic Feedback:** Configurable vibration tick when swiping down from the status bar.
- **Zero-Friction Pass-Through:** Full pass-through for taps and horizontal gestures to underlying status bar elements.

### 4. Display Fix
- **Aspect Ratio & Density Tuning:** Applies calibrated display resolution (`wm size`) and density (`wm density`) overrides via Shizuku to optimize app scaling and layout on the Titan 2 Elite's unique physical form factor.
- **Snapshot & Restore:** Captures prior system settings before modification and provides 1-tap restoration.
- **Automatic Boot Persistence:** Re-applies display configuration on system boot via WorkManager.

---

## Requirements

- **Device:** Unihertz Titan 2 Elite (Android 12+, tested through Android 16 / API 36).
- **[Shizuku](https://shizuku.rikka.app/):** Required for privileged shell operations (Display Fix, 1-tap Accessibility enabling, and DND policy grants) without requiring root.
- **Accessibility Service:** Required for the Unified Pull-Down swipe detection and Status Bar Flashlight Indicator overlay.

---

## Installation & Setup

1. Download the latest release APK from [Releases](https://github.com/eqbirvin/T2EToolbox/releases) or the `releases/` directory.
2. Install via ADB:
   ```bash
   adb install -r releases/t2e.toolbox.v0.5.0.apk
   ```
3. Open **T2E Toolbox** and follow the in-app setup:
   - Start **Shizuku** and authorize T2E Toolbox.
   - Enable the **Unified Pull-Down** accessibility service (1-tap via Shizuku or via Android Settings).
   - If using the **Volume Mode Switcher**, map the **Volume Mode** app to your physical programmable side key in **Settings $\rightarrow$ Programmable Key**.

---

## Building from Source

### Prerequisites
- JDK 17
- Android SDK (API 35/36)

### Gradle Tasks
```bash
# Compile release Kotlin code
./gradlew compileReleaseKotlin

# Run JVM unit tests
./gradlew test

# Assemble and copy minified release APK to releases/
./gradlew copyReleaseApk
```

---

## Architecture & Code Quality

- **Modern Android Stack:** Jetpack Compose (Material 3), Kotlin Coroutines & StateFlow, Navigation Compose.
- **Clean Architecture:** Strict separation between UI, ViewModels, and Data Repositories; zero privileged or IPC operations on the Main thread (`Dispatchers.IO` and serialized dispatchers).
- **Safety & Performance:** ProGuard / R8 minification with strict resource shrinking enabled.
