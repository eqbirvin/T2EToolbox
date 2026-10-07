# Changelog

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
