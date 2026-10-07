# Changelog

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
