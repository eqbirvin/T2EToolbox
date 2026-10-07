# Changelog

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
