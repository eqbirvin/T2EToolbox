# Changelog

## [0.12.4] - 2026-10-19
### Fixed
- **Per-App DPI — App Refresh Loop on Volume Keys, Notification Shade, Permission Prompts & Overlays**:
  - **Root Cause**: `com.android.systemui` was treated like Home/Recents, and every other window-state change was treated as a new foreground app. Any overlay drawn on top of a scaled app (volume panel, notification shade / Quick Settings, power menu, screenshot UI, permission prompts, Circle to Search, Assistant / Gemini overlay) therefore triggered `wm density reset`. The scaled app (e.g. Instagram at Tablet 600 dp) relaunched, reported itself foreground again, and had its DPI re-applied, causing a double refresh. Because SystemUI also rebuilds the volume dialog on density changes, this repeated on **every** volume key press.
  - **Fix**: Added `TransientOverlayClassifier`, and `PerAppDpiRepository.onPackageForeground` now ignores transient overlays entirely (like IMEs), keeping the current app's density while they are shown:
    - **SystemUI** windows (volume panel, shade, Quick Settings, power menu, screenshot UI, etc.).
    - **Permission prompts** (`permissioncontroller`) and **install / uninstall dialogs** (`packageinstaller`).
    - **Non-application windows**: the accessibility service now resolves the event's `AccessibilityWindowInfo` type, and anything that isn't `TYPE_APPLICATION` (voice-interaction sessions used by **Circle to Search** and the **Assistant / Gemini overlay**, floating chat bubbles, system dialogs) is ignored.
    - **Google app overlay fallback**: when window metadata isn't available yet, Google app windows whose class indicates Lens / Assistant / Gemini are ignored.
  - Lockscreen safety is unchanged and still handled by the `ACTION_SCREEN_OFF` restore and keyguard guard.

## [0.12.3] - 2026-10-18
### Added
- **Block Scroll While Typing — Extended Debounce Duration (Up to 10 Seconds)**:
  - Expanded the configurable keystroke debounce window from `300ms..2500ms` up to **10 seconds (`300ms..10000ms`)** in 100ms increments.
  - Added quick preset chips for `400ms`, `800ms`, `2s`, `5s`, and `10s`.
- **Zero-Idle-Drain IME Race Guard**:
  - Replaced the continuous 200ms property watchdog loop with a bounded 3-step race guard (`150ms`, `250ms`, `400ms` after focus/typing transitions) so leaving a text field open indefinitely consumes **0.0% background CPU or battery**.

## [0.12.2] - 2026-10-18
### Fixed
- **Block Scroll While Typing — Native `findTouchedWindowAt` `"InputMethod"` Sink & Compose Focus Fix**:
  - **Root Cause (`libinputflinger.so` `0x203bd8` & `0x205184`)**: When `debug.sf.agui_input_status == "1"`, Unihertz's `InputDispatcher::notifyMotion` keeps `source = AINPUT_SOURCE_TOUCHPAD` and `dispatchMotionLocked` (`AGOLD:dispatchMotionLocked >>>>>> Find target window (InputMethod) !!!!`) directs `findTouchedWindowAt` to find a window whose title contains `"InputMethod"` to absorb touchpad motion events. When using a zero-window physical keyboard IME like **PhysiBoard**, no `"InputMethod"` window exists in `InputDispatcher`, causing `findTouchedWindowAt` to fall through to the foreground app window and scroll it.
  - **1×1 `"InputMethod"` Touchpad Sink Overlay**: Added a 1×1 transparent, `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE` `TYPE_ACCESSIBILITY_OVERLAY` window titled `"InputMethod"` in `TypingScrollBlockManager`. When `debug.sf.agui_input_status == "1"`, `findTouchedWindowAt` (`0x2051d0`) matches this sink window *before* checking `FLAG_NOT_TOUCHABLE` (`0x205214`) and routes touchpad swipes into the sink `View` where they are consumed and dropped. When `debug.sf.agui_input_status == "0"` (or for any normal `AINPUT_SOURCE_TOUCHSCREEN` touch), `findTouchedWindowAt` skips the `"InputMethod"` check and obeys `FLAG_NOT_TOUCHABLE`, leaving normal scrolling and touchscreen touches 100% unaffected.
  - **Compose & Popup Focus Retention**: Fixed `handleFocusEvent` prematurely clearing text field focus when secondary `TYPE_VIEW_TEXT_SELECTION_CHANGED` or container/popup focus events fire with a `null` or non-editable `event.source` while an editable input field still holds `FOCUS_INPUT` in `rootInActiveWindow`.
  - **Mutex-Serialized Property Sync & 200ms Watchdog**: Serialized `debug.sf.agui_input_status` updates with a coroutine `Mutex` and a 200ms shared-memory watchdog while claimed.

## [0.12.1] - 2026-10-18
### Fixed
- **Block Scroll While Typing on Unihertz Titan 2 Elite (`libinputflinger.so` & PhysiBoard Fix)**:
  - Fixed an issue where Unihertz's native C++ `InputDispatcher` (`libinputflinger.so`) bypassed Android's `AccessibilityInputFilter` (`SOURCE_TOUCHPAD`) whenever Scroll Assistant (`persist.sys.scroll_assistant = 1`) was enabled.
  - Synchronizes Unihertz's native volatile RAM input flag (`debug.sf.agui_input_status`) via Shizuku on a serialized background IO dispatcher (`1` = suppress Scroll Assistant in `InputDispatcher::notifyMotion`, `0` = allow Scroll Assistant), with zero NAND flash storage wear (`u:object_r:debug_prop:s0` resides strictly in volatile `tmpfs` `/dev/__properties__`).
  - Added an IME race guard (120ms/300ms shared-memory `SystemProperties` check) so zero-height physical keyboard IMEs like **PhysiBoard** (`mImeWindowVis = 0`) cannot reset `debug.sf.agui_input_status` back to `0` via `InputMethodManagerService.setImeWindowStatusLocked` after a text field gains focus.
  - Added `TYPE_VIEW_TEXT_CHANGED` accessibility event monitoring alongside `onKeyEvent` so keystroke debounce and text-field detection work reliably even when a higher-priority physical keyboard AccessibilityService (such as PhysiBoard) consumes raw `KeyEvent`s upstream.

## [0.12.0] - 2026-10-18
### Added
- **Dual-Methodology Typing Scroll Blocking for Unihertz Titan 2 Elite**:
  - Added continuous text field focus suppression alongside keystroke debounce suppression, allowing users to choose either or both methodologies:
    - **Method 1: While Actively Typing (Debounce)**: Suppresses capacitive keyboard trackpad scroll upon physical keystrokes, automatically releasing after a configurable rolling delay (300ms–2500ms).
    - **Method 2: While Text Field Focused (Continuous)**: Dynamically suppresses capacitive keyboard trackpad scrolling whenever an editable text field (`EditText`, Compose `BasicTextField`, or web inputs) is focused on screen.
  - **In-Memory Input Stream Control**:
    - Both methodologies operate dynamically via `motionEventSources = SOURCE_TOUCHPAD` in Android's Accessibility framework.
    - Zero flash wear (no writing to NAND `/data/property` via `setprop`), sub-millisecond latency, and zero root or Shizuku requirement.
    - Display glass touchscreen touches remain 100% unaffected.
  - **Enhanced Interactive Diagnostics UI**:
    - Dual toggle switches for both blocking methodologies.
    - Real-time status chips indicating focus state ("Focus: Field Active" vs "Focus: None") and debounce state ("Keystroke: Typing" vs "Keystroke: Idle").
    - Composite status badge clearly showing whether keyboard scrolling is currently suppressed or allowed.

## [0.10.0] - 2026-10-16
### Added
- Added "Block Scroll While Typing" feature for Unihertz Titan 2 Elite:
  - Eliminates accidental scrolling and page bouncing caused by thumb swipes while typing on the capacitive physical keyboard.
  - **Dynamic Input Filtering via Accessibility Framework**:
    - Listens for physical keystrokes (`canRequestFilterKeyEvents` and `FLAG_REQUEST_FILTER_KEY_EVENTS`).
    - Dynamically claims `InputDevice.SOURCE_TOUCHPAD` on the active accessibility service when physical typing begins, routing capacitive swipes to `onMotionEvent()` where they are silently discarded by the system `InputDispatcher`.
    - Normal display glass touches (`SOURCE_TOUCHSCREEN`) remain 100% unaffected.
    - Physical keystrokes are never consumed or delayed (`onKeyEvent` returns `false`).
  - **Rolling Debounce Window & Automatic Restoration**:
    - Configurable delay (300ms–2500ms, default 800ms) with presets for Fast (400ms), Default (800ms), Deliberate (1200ms), and Relaxed (1600ms) typists.
    - Automatically clears touchpad interception when typing pauses, instantly restoring smooth trackpad scrolling for reading and web browsing.
  - **Interactive Live Typing Test Area**:
    - Built-in live test field in `TypingScrollBlockScreen` showing real-time feedback badges ("Typing Detected — Trackpad Scroll Blocked!" vs "Idle — Trackpad Scroll Allowed") to easily test and tune debounce settings.
  - Zero root or Shizuku requirement; runs completely in-memory in Android's input pipeline.

## [0.9.0] - 2026-10-16
### Added
- Added "Circle to Search Cleanup" tool & automatic cleaner for Unihertz Titan 2 Elite:
  - Solves Unihertz's Circle to Search (`AguiAiCircleSearch.apk`) flaw where screen captures and cropped searches accumulate indefinitely in `/sdcard/Pictures/AguiCircleSearch/` without deletion.
  - **Automatic Cleanup Observer**:
    - Listens for CTS completion via `Settings.Global` (`agui_circle_search_finishing`) and accessibility window events transitioning to Google Lens.
    - Employs a configurable safety delay (default 4 seconds) to guarantee Google Lens has completely ingested the image into memory before purging.
    - Deletes orphaned screenshot files on disk and purges broken MediaStore records via privileged Shizuku commands to prevent broken gallery thumbnails.
  - **Manual Storage Management UI**:
    - Real-time disk scanner displaying active count and reclaimed storage footprint.
    - One-tap "Clean Storage Now" action with instant feedback.
  - **Boot Cleanup**:
    - Automatically sweeps stale CTS screenshots upon device boot via `DisplayFixBootWorker`.

## [0.8.0] - 2026-10-16
### Added
- Added "Keyboard Backlight Control" for Unihertz Titan 2 Elite:
  - Direct integration with Unihertz vendor framework service (`agui_functional_service`) via Shizuku binder calls.
  - **Configurable Backlight Timeout**:
    - Stepped slider and direct numeric input supporting timeouts from **5 seconds up to 5 minutes (300s)**.
    - Quick preset chips for rapid selection (15s, 30s, 1m, 2m, 5m).
  - **"Keep On With Screen" Option**:
    - Keeps the physical keyboard backlight illuminated continuously while the display remains on by setting vendor backlight timeout to 24 hours.
    - Synchronized with screen state via `ACTION_SCREEN_ON` and `ACTION_SCREEN_OFF`: immediately extinguishes the keyboard LEDs via `agui.action.CLOSE_KEYBOARD_LIGHT` broadcast when the display turns off, and wakes the LEDs via synthetic keyevent 0 when the display wakes.
  - **Brightness Control**:
    - Integrated brightness slider (0–100%) adjusting the keyboard LED duty cycle directly through `agui_functional_service`.
  - **Live Hardware Status & Persistence**:
    - Reads active hardware timeout and brightness directly from vendor service and displays live status indicators.
    - Restores custom backlight configuration automatically across device reboots via `DisplayFixBootWorker`.
    - Real-time updates with immediate apply buttons and feedback snackbars.

## [0.7.2] - 2026-10-15
### Fixed
- Fixed base density corruption and keyboard IME switching in Per-App Scaling:
  - **IME / Keyboard Filtering**: Added active input method detection (`InputMethodManager.enabledInputMethodList`) to ignore keyboard popups (e.g. PhysiBoard, Gboard) during texting/typing, ensuring app scaling stays locked to tablet layout and does not trigger spurious app switches.
  - **Base Density Override Protection**: Introduced atomic `isOverridingDpi` state tracking to prevent WindowManager's asynchronous `onConfigurationChanged` callback from overwriting base density with the temporary target DPI during density transitions.
  - **Physical Density Detection & Safe Reset**: Added direct physical density detection via `wm density` output; when returning to base without user custom override, reliably executes `wm density reset` to cleanly restore stock hardware density.
  - **Comprehensive Home Launcher Detection**: Enhanced launcher package resolution to query both `queryIntentActivities` and `resolveActivity`, ensuring third-party launchers like Nova Launcher trigger immediate reversion to base density.

## [0.7.1] - 2026-10-15
### Fixed
- Fixed app picker only displaying ~5 apps in Per-App Scaling:
  - Added `android.permission.QUERY_ALL_PACKAGES` permission and explicit `<intent>` filters (`CATEGORY_LAUNCHER` and `CATEGORY_HOME`) to `AndroidManifest.xml` to bypass Android 11+ Package Visibility filtering.
  - Enhanced installed app resolution to query with `PackageManager.MATCH_ALL` and fall back to `getInstalledApplications` with `getLaunchIntentForPackage`, ensuring all user-installed and system launcher apps appear in the searchable app picker.

## [0.7.0] - 2026-10-15
### Added
- Added "Per-App Scaling (DPI)" feature for Unihertz Titan 2 Elite:
  - Dynamically overrides screen density (DPI / smallest width $sw$) on a per-app basis via Shizuku window manager commands.
  - Solves vertical squishing and content cutoff issues on Titan 2 Elite's unique nearly square display ($1080 \times 1200$, physical $\approx 576\text{ dp}$ phone layout) by triggering Android's native tablet layout ($\ge 600\text{ dp}$) for apps like Instagram, Spotify, and Slack.
  - **Intelligent Layout Range Legend**: Dynamically computes and displays layout ranges for the active screen resolution (Stock 1080px, Calibrated 1076px, or 864px Display Fix):
    - **Phone Layout**: $< 600\text{ dp}$ ($\ge 289\text{ DPI}$ at stock 1080px)
    - **Tablet Layout (Recommended)**: $600\text{--}719\text{ dp}$ ($241\text{--}288\text{ DPI}$ at stock 1080px)
    - **Desktop Layout**: $\ge 720\text{ dp}$ ($\le 240\text{ DPI}$ at stock 1080px)
  - **Custom Scaling Modes**: Presets for Tablet ($600\text{ dp}$), Desktop ($720\text{ dp}$), Custom Smallest Width (dp), and Custom Density (DPI) with real-time in-dialog calculation previews.
  - **Automatic Base Density Restoration**: Intelligently captures base density configured in Settings, Developer Options, or ADB, and restores it immediately upon entering Home launcher, Recents screen, or switching to untargeted apps.
  - **Debounced Window Tracking**: Low $180\text{ ms}$ settle debounce provides rapid, responsive switching without animation jitter during fast app switching.
  - **Lockscreen & Pedometer Safety**: Reverts to base density immediately upon screen-off (`ACTION_SCREEN_OFF`) to prevent Unihertz lockscreen distortion.
  - **Searchable App Picker**: Filter through all installed launcher applications with app icons and labels.
  - Hosted directly within the existing `UnifiedPullDownAccessibilityService` for window state observation without additional background services.

## [0.6.2] - 2026-10-15
### Fixed
- Fixed status bar icon visibility in T2E Toolbox app:
  - Configured `windowLightStatusBar = true` and `windowLightNavigationBar = true` in `Theme.T2EToolbox`.
  - Added programmatic insets controller enforcement (`WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true`) in `MainActivity`.
  - Ensures status bar foreground elements (clock, battery level, cellular/Wi-Fi signal, and notification icons) render in dark/black for high contrast against the app's light background, even when system-wide dark mode is enabled on the device.

## [0.6.1] - 2026-10-15
### Fixed
- Fixed "Silent (Mute only)" setting inadvertently triggering Do Not Disturb (DND):
  - Solved AOSP audio architecture limitation where public `AudioManager.setRingerMode(RINGER_MODE_SILENT)` forces Zen Mode (`ZEN_MODE_IMPORTANT_INTERRUPTIONS`).
  - Added privileged Shizuku-backed ringer control using internal audio service commands (`cmd audio set-ringer-mode SILENT && cmd notification set_dnd off`), keeping DND completely disabled while muting ring, notification, system, and DTMF streams to volume 0 with vibration off.
  - Ensured visual heads-up notifications, lockscreen banners, and status bar icons continue to appear normally without suppression.
  - Updated ringer state evaluation in cycling logic to use unified `readCurrentRingerMode()`, preventing out-of-sync cycle steps.
  - Added fallback volume stream muting and unmuting for environments where Shizuku is not running.

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
