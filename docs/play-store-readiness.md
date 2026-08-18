# Play Store readiness

Whispry currently distributes via GitHub Releases + an in-app OTA updater (see README). This doc
tracks what changes before a Play Store submission would be viable, and gives ready-to-paste text
for the Play Console forms that need it.

## Blocker 1: self-updater conflicts with Play policy

`UpdateDownloader` / `UpdateInstaller` (see `app/src/main/java/com/example/whispry/updater/`)
download an APK from GitHub Releases and install it via `REQUEST_INSTALL_PACKAGES`. Google Play's
Device and Network Abuse policy prohibits apps distributed through Play from updating themselves
by any mechanism other than Play's own update flow. An app built exactly as-is today will be
rejected (or removed post-approval) for this, independent of the accessibility question below.

There's no way to keep this behavior in a Play-distributed build. Before submitting, the updater
needs to become conditional: keep it for GitHub builds, strip it (and the `REQUEST_INSTALL_PACKAGES`
permission, and the "Updates" settings screen) for a Play build. The standard way to do this is a
Gradle product flavor split (`github` / `play`), with the updater code and permission only present
in the `github` flavor. That's a real implementation task, not a config toggle — flag it before
scheduling a submission date.

## Blocker 2: Accessibility Service justification

`TriggerService` (`app/src/main/java/com/example/whispry/service/TriggerService.kt`) is an
`AccessibilityService`. Play requires the **Accessibility API Permission Declaration Form** in
Play Console for any app requesting `BIND_ACCESSIBILITY_SERVICE`, and reviews it manually. Below
is ready-to-paste text for that form, reflecting what the service actually does as of this pass
(volume-key trigger is implemented but no longer offered in Settings, see
`TriggerRepositoryImpl.getAvailableTriggerModes()`).

### "What does your app do?"

> Whispry is a voice-dictation tool. The user speaks, and Whispry types the transcribed, AI-formatted
> text directly into whatever text field they're currently focused on, in any app, without the user
> having to manually copy and paste.

### "Why does your app need the Accessibility API?"

> Whispry needs two things no other public Android API provides:
>
> 1. **Inserting transcribed text into the focused field of an arbitrary third-party app.** There is
>    no general-purpose API for one app to type into another app's text field. `AccessibilityNodeInfo`
>    (via `ACTION_PASTE` on the currently-focused, editable node) is the only mechanism that works
>    across arbitrary target apps. Without it, Whispry could only copy text to the clipboard and
>    require the user to manually paste after every dictation, which defeats the app's core purpose
>    (hands-free, uninterrupted dictation).
> 2. **Detecting the on-screen keyboard's position.** Whispry's keyboard-riding trigger button has to
>    float directly above the user's IME. `AccessibilityWindowInfo` (TYPE_INPUT_METHOD window bounds)
>    is the only way to get this outside the app that owns the keyboard.
>
> The service also reads `TYPE_WINDOW_STATE_CHANGED` events for the foreground package name only
> (not window content), used to: (a) suppress the floating trigger widget in apps the user has
> explicitly hidden it from in Settings, and (b) support one optional voice command ("calculate")
> that taps buttons in the system calculator app the user just launched via voice.
>
> The service does not read, log, store, or transmit screen content from other apps. It reads the
> focused editable node (to paste into it) and window/foreground-package metadata only, and none of
> that data leaves the device.

### "Why can't you use a less invasive permission/API?"

> There is no alternative API for cross-app text insertion or IME-bounds detection on Android. An
> input-method (`InputMethodService`) can insert text but only while it is itself the active
> keyboard, which would require Whispry to replace the user's keyboard entirely rather than
> layering on top of it, a fundamentally different (and much worse) product for users who want to
> keep their existing keyboard.

## Other findings

- **`targetSdk` gap**: `app/build.gradle.kts` sets `compileSdk = 37`, `targetSdk = 34`. Play requires
  targeting within one platform version of current at submission time; 34 is very likely stale by
  now. Bumping `targetSdk` can change runtime behavior (foreground service rules, permission
  behavior, etc.) and needs a real device-test pass, not a blind bump. Do this deliberately, close
  to submission time, with the changelog for the target API level open next to you.
- **`SYSTEM_ALERT_WINDOW`** (draw-over-other-apps, for the floating widget) is a Play-restricted
  permission. It's allowed, but Play Console will ask you to declare its use case in the "Restricted
  permissions" section of the Play Console policy declarations, similar to the accessibility form.
  Use the same "floating trigger + recording bubble" framing as the in-app permission card
  (`PermissionsScreen.kt`) already uses.
- **Privacy policy URL**: Play Console requires a hosted privacy policy URL in the store listing.
  The README's [Privacy](../README.md#-privacy) section can serve as that URL
  (`https://github.com/cosmictaserdev-creator/whispry#-privacy`) since GitHub renders it publicly,
  but confirm the anchor still resolves after any future README restructuring.
- **Data safety form**: based on current code, answers should be: no data collected/shared, audio
  and transcript text leave the device only to the user's own configured AI provider (not to
  Whispry), no analytics/crash SDK. Cross-check against whatever's true at submission time.

## Not done in this pass

Flavor-splitting the updater and bumping `targetSdk` are real engineering changes with runtime and
build-graph implications, not something to do silently as part of a cleanup pass. This doc exists so
they're tracked instead of discovered at submission time.
