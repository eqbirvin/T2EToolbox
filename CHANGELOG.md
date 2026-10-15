# Changelog

## [0.6.0] - 2026-10-15
### Added
- Added "Silent Mode Behavior" setting to Volume Mode Switcher:
  - Users can now explicitly choose what happens when cycling into the Silent state:
    - **Silent (Mute only)**: Mutes calls, notifications, and vibrations while keeping Do Not Disturb OFF, ensuring visual notification banners, status bar icons, and lockscreen alerts display normally.
    - **Do Not Disturb (DND)**: Activates system Do Not Disturb (`INTERRUPTION_FILTER_PRIORITY`), suppressing visual notifications according to device DND rules.
  - Dynamically updates active status card (🔇 Silent vs ⛔ Do Not Disturb), cycle sequence labels, haptic vibration cues, and on-screen Toast alerts.
  - Automatically synchronizes with system DND toggle changes via `ACTION_INTERRUPTION_FILTER_CHANGED`.
  - Guarantees DND is disabled when switching back to Sound or Vibrate modes.

## [0.5.1] - 2026-10-15
### Fixed
- Fixed Volume Mode Switcher pulling T2E Toolbox main app to foreground: added `android:taskAffinity=""`, `android:launchMode="singleInstance"`, and `finishAndRemoveTask()` to `VolumeModeActivity` so the headless shortcut operates in its own task and never surfaces `MainActivity`.
- Added graceful permission fallback to Volume Mode cycling: when Do Not Disturb permission is not granted, attempting to cycle into Silent mode gracefully falls back to Sound mode instead of deadlocking the hardware shortcut in Vibrate mode.

## [0.5.0] - 2026-10-13
### Added
- Added "Flashlight Indicator" feature:
  - Displays a clean horizontal flashlight badge in the dead center of the status bar whenever the flashlight is active.
  - Helps prevent accidental battery drain on the Unihertz Titan 2 Elite when the programmable side key is mapped to toggle the flashlight.
  - Draws directly over the status bar using `TYPE_ACCESSIBILITY_OVERLAY` hosted seamlessly within `UnifiedPullDownAccessibilityService`.
  - Non-intrusive touch behavior: transparent touch pass-through (`FLAG_NOT_TOUCHABLE`) allows status bar gestures and shade expansion to work unhindered.
  - Global torch monitoring via `CameraManager.TorchCallback` tracking activation from physical buttons, Quick Settings tiles, or third-party camera apps.
  - Customizable indicator colors: Amber Glow (default), Bright Yellow, and Crisp White.
  - Optional "Tap Indicator to Turn Off" setting enabling instant touch dismissal.
  - In-app interactive testing controls and Titan 2 Elite hardware button protection guide.

## [0.4.0] - 2026-10-09
### Added
- Added "Volume Mode Switcher" feature:
  - Adds an invisible "Volume Mode" launcher app shortcut designed for the Unihertz Titan 2 Elite programmable side key (Settings -> Programmable Key -> Launch app).
  - Pressing the physical side key cycles smoothly through Sound, Vibrate, and Silent modes for the phone without opening any app screen.
  - Transparent activity (`Theme.Transparent`) with zero window animations, completing within milliseconds and finishing immediately.
  - Distinct haptic vibration cues tailored to each mode (double pulse on vibrate, crisp tick on sound, subtle tap on silent).
  - Optional on-screen Toast indicator displaying the active sound mode.
  - Configurable cycle sequences in T2E Toolbox: Sound -> Vibrate -> Silent (default), Sound <-> Vibrate, Sound <-> Silent, Vibrate <-> Silent.
  - 1-tap Do Not Disturb permission granting via Shizuku (`cmd notification allow_dnd`) with system settings fallback.
  - In-app interactive testing controls and setup guide for the Titan 2 Elite.

## [0.3.2] - 2026-10-07
### Fixed
- Fixed shell injection vulnerability when modifying `enabled_accessibility_services` or display overrides by introducing POSIX single-quote escaping (`ShizukuShell.shellQuote()`) with unit test coverage.
- Fixed dead restore logic in `disableWithShizuku()`: properly restores `accessibility_enabled` from snapshot (or deletes setting if null), checks exit codes, and only clears snapshot after success.
- Fixed main-thread binder/IPC calls during shade detection by moving detection, action resolution, and action execution to a dedicated background serialized dispatcher (`swipeDispatcher`).
- Made `PullDownStateMachine` thread-safe with `@Synchronized` annotations to prevent race conditions during rapid gestures or window switches.
- Reduced battery usage by removing noisy `typeWindowContentChanged` events from the accessibility service configuration.
- Updated accessibility service description string in `strings.xml` to accurately describe window reading scope and transparency.
- Wrapped display fix shell calls and permission requests in `withContext(Dispatchers.IO)` in `DisplayFixRepository`.
- Fixed service rebind lifecycle leaks by scoping settings flow collectors to a connection-bound `Job` canceled on `onUnbind()`, and guarding overlay attachment against unready states or duplicate views.
- Removed legacy `temp_shizuku/` scratch directory from workspace.
### Added
- Added "Vibrate on shade expansion" setting to Unified Pull-Down:
  - Allows users to enable or disable the haptic tick feedback triggered when swiping down from the status bar.
  - Defaults to enabled, backed by `SettingsRepository` and reactive StateFlow across UI and service.

## [0.3.0] - 2026-10-07
### Added
- Added "Smart Shade Detection" setting to Unified Pull-Down:
  - Dynamically inspects SystemUI window hierarchy to detect whether the shade is closed, displaying notifications, or expanded to Quick Settings.
  - When ON, swipes down cycle smoothly without a timeout: Closed → Notifications → Quick Settings → Notifications.
  - When OFF, retains the 3-second timeout state machine alternating between notifications and Quick Settings.
- Added pure resolver logic `resolveSmartAction` and comprehensive unit test coverage.
- Extracted modular `ShadeStateDetector` to keep accessibility service lean and maintainable.
- Added in-app setting toggle and descriptive UI guidance.

## [0.2.3] - 2026-10-07
### Fixed
- Fixed overlay getting killed on status bar tap and restored debug area tint by removing fragile window insets visibility listener that was falsely setting the overlay to `INVISIBLE` and destroying its surface.
- Improved tap pass-through responsiveness by treating all non-downward gestures on `ACTION_UP` as underlying status bar taps.
- Added automatic shade dismissal detection: transitioning away from SystemUI window immediately resets the pull-down state machine to opening notifications.

## [0.2.2] - 2026-10-07
### Fixed
- Fixed tap pass-through crash by appending a non-empty line segment to `Path` and wrapping gesture creation and dispatch in structured exception handling, preventing service termination and ensuring touchability restoration.
- Fixed in-app toggle re-activation by nulling the detached overlay view reference so toggling Unified Pull-Down cleanly adds a fresh view to WindowManager.
- Restored second-swipe timeout set to 3 seconds (`3000ms`), after which pull-down resets to opening the notification shade.
- Updated Unified Pull-Down UI copy to indicate the 3-second window.

## [0.2.1] - 2026-10-06
### Fixed
- Fixed overlay disappearing permanently after turning off the screen by recovering status bar visibility on screen-on/user-present, requesting insets on wake, and using `View.INVISIBLE` to preserve insets dispatch.
- Fixed taps being swallowed by the overlay; taps on status bar elements are now cleanly passed through to the system via `dispatchGesture`.
- Removed the 4-second timeout for the second swipe down: swiping down alternates between notifications and Quick Settings naturally, resetting back to notifications on screen-off.
- Updated Unified Pull-Down UI copy to reflect the unconstrained second-swipe behavior.

## [0.2.0] - 2026-10-06
### Added
- Added Unified Pull-Down feature: Replaces vendor split status bar gesture with stock Android behavior (swiping anywhere opens notifications; swiping again within 4s opens Quick Settings).
- Implemented `UnifiedPullDownAccessibilityService` using a full-width `TYPE_ACCESSIBILITY_OVERLAY` to intercept and consume status bar touches cleanly.
- Added pure Kotlin gesture state machine with haptic tick feedback.
- Added Shizuku shell fallback execution (`cmd statusbar expand-notifications` and `cmd statusbar expand-settings`).
- Added one-tap Shizuku accessibility service enabler with safe snapshot-and-restore.
- Added lockscreen visibility toggle, debug visual overlay tint, and interactive test buttons.

## [0.1.1] - 2026-10-06
### Fixed
- Fixed Display Fix apply-at-boot by correcting receiver component registration and executing background wait/apply via WorkManager.
- Fixed Shizuku detection and reactive status updates on Android 11+ via package queries declaration and lifecycle listeners.
- Fixed Shizuku shell command execution in release builds by adding correct ProGuard keep rules for reflection.

## [0.1.0] - 2026-10-06
### Added
- Initial release of T2E Toolbox.
- Display Fix feature: Change resolution to 864x960 at 231 dpi via Shizuku.
- Apply at boot functionality for Display Fix.
