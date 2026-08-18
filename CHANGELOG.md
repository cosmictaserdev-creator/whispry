# Changelog

All notable changes to Whispry are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/) —
`MAJOR.MINOR.PATCH`.

## [Unreleased]

## [1.3.0] - 2026-08-18

### Added
- Upload audio to transcribe: pick an existing audio file from History
  (flac, mp3, mp4, m4a, ogg, wav, webm, up to 25MB) instead of recording
  live. Transcription runs in the background via WorkManager, so it
  survives the app being backgrounded or killed mid-upload, and a
  notification links back to History once the transcript is ready.
- In-app OTA update checker: Settings → Service & Maintenance → Updates,
  checks GitHub Releases, shows changelog, downloads and installs the
  signed APK in place.
- Auto-versioning: `versionName`/`versionCode` now derive from the git tag
  (`vX.Y.Z`) instead of a hand-maintained number in `build.gradle.kts` —
  one less place a release can drift out of sync with its own tag.
- On-device crash logging (Timber + a local uncaught-exception handler):
  About → Share Crash Log lets you attach the last crash's stack trace to
  a bug report. Nothing is collected or uploaded automatically.
- Open-source release scaffolding: LICENSE (AGPL-3.0 + attribution term),
  NOTICE, CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, issue/PR templates,
  signed-release GitHub Actions workflow.

### Fixed
- Settings cards no longer show a touch glow (kept the press scale/stretch).
- Floating widget's edit-mode "Position & size" card can now be dragged
  aside instead of blocking the widget underneath.
- Floating widget's overlay window now leaves enough slack for the
  recording-time size pop, instead of clipping it.
- Floating widget no longer visibly jumps when it shrinks back to its
  idle edge sliver after a recording ends.

### Added (widget)
- Slim accent-colored outline on the floating widget while actively
  recording.

## [1.0.0] — first tagged release

Initial public release.
