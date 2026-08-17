# Whispry — Slice Implementation Log (#29–#36)

Implementation log for the 8 vertical-slice issues (#29–#36). All slices are
implemented, compile clean (`.\gradlew.bat :app:compileDebugKotlin -q`), and the
unit test suite passes (`.\gradlew.bat :app:testDebugUnitTest -q`). **Nothing is
device-verified yet** — acceptance criteria are runtime behaviors, so the GitHub
issues stay open until tested on a device.

Branch: `feature/i18n-and-verification`. Compile SDK 37, target SDK 34.
Issue bodies: `C:\Users\cosmi\AppData\Local\Temp\opencode\issues\slice1.md`–`slice8.md`.

---

## #30 Keyboard default + volume demotion + settings reorg

- `presentation/settings/SettingsScreen.kt` — volume trigger copy → "Optional — double press and hold $keyName" (volume no longer the hero trigger)
- `presentation/onboarding/PermissionsScreen.kt` — accessibility copy reworded to keyboard-centric (mic above keyboard, paste)
- `presentation/settings/SettingsContract.kt` — `SettingsState.triggerMode` default → `TriggerMode.Manual`
- `data/repository/TriggerRepositoryTest.kt` — default-mode test now asserts Manual

## #32 Keyboard logo: free 2D drag + keyboard-ride + persisted position

- `data/local/datasource/DataStoreKeys.kt` — `KEYBOARD_LOGO_Y_OFFSET` key (vertical anchor so the pill rides the IME)
- `service/BubbleService.kt` — `keyboardLogoYOffsetPx` field; `positionKeyboardLogo()` clamps offset to ≥8dp margin so the pill never covers keys; `moveKeyboardLogo(dx, dy)` free 2D drag; `persistKeyboardLogoPosition()` saves X% + Y offset on drag end, snaps up if dropped on keys
- `service/KeyboardLogoSurface.kt` — `detectDragGestures` 2D, wired `onDrag` / `onDragEnd`

## #31 Keyboard logo: double-tap preset submenu

- `service/KeyboardLogoSurface.kt` — rewritten: `detectTapGestures(onDoubleTap, onTap)` + new `KeyboardLogoPresetPanel` (grouped preset picker, scale-in)
- `service/BubbleService.kt` — `PRESET_PANEL_WIDTH_DP`/`PRESET_PANEL_HEIGHT_DP`; `showKeyboardLogoPresetPanel()` / `hideKeyboardLogoPresetPanel()` (separate overlay window anchored to the pill, flips up/down to avoid clipping); `selectPresetFromKeyboardLogo()` writes `DEFAULT_OUTPUT_PRESET`; panel dismissed on recording start / drag start
- `service/ServiceLocator.kt` — compile fix: API 37 removed `AccessibilityWindowInfo.getPackageName()`, use root node's packageName

## #34 RAMP widget: keyboard-overlap nudge

- `data/local/datasource/DataStoreKeys.kt` — `WIDGET_AVOID_KEYBOARD` (default true)
- `service/WidgetConfig.kt` — `avoidKeyboard` field read from prefs
- `service/FloatingWidgetManager.kt` — `observeImeForKeyboardNudge()` (collects `serviceBridge.imeBounds`), `restingY()` denormalizes the saved position, `slideWidgetTo()` 160ms ease-out, `KEYBOARD_NUDGE_MARGIN_DP = 8`; `observeEnabled` now combines widgetEnabled + widgetsHidden
- `presentation/settings/SettingsScreen.kt` + `SettingsContract.kt` + `SettingsViewModel.kt` — "Avoid keyboard overlap" toggle wired

## #35 Hidden apps list (both widgets)

- `data/local/datasource/DataStoreKeys.kt` — `HIDDEN_APPS` set key
- `service/ServiceBridge.kt` — `foregroundPackage` StateFlow + `setForegroundPackage()`
- `service/WindowOverlayCoordinator.kt` — rewritten: constructor now `(settingsProvider, serviceBridge)`; exposes `widgetsHidden` = combine(HIDDEN_APPS set + foreground package)
- `service/TriggerService.kt` — emits foreground package on `TYPE_WINDOW_STATE_CHANGED`
- `service/FloatingWidgetManager.kt` — floating widget hides when `widgetsHidden`
- `service/BubbleService.kt` — keyboard logo hides when `widgetsHidden`
- `features/hiddenapps/presentation/HiddenAppsViewModel.kt` — created: hiddenApps set + installed-app list (LAUNCHER intent, own package excluded), `setHidden()` toggle
- `features/hiddenapps/presentation/HiddenAppsScreen.kt` — created: hidden-app list with remove + searchable add-app sheet
- `navigation/Routes.kt` + `navigation/WhispryNavHost.kt` — `Route.HiddenApps` + registration
- `ui/components/WhispryDetail.kt` — `WhispryHeroKeys.HiddenApps`
- `presentation/settings/SettingsScreen.kt` — "Hidden apps" row under the widget section

## #36 Notifications: silent service pill + optional premium alerts

- `notification/NotificationChannels.kt` — FGS channel → `IMPORTANCE_NONE` (service stays running, notification invisible in the shade)
- `notification/WhispryNotificationManager.kt` — removed Samsung Now Bar extras + `setProgress` usage-progress decoration; fallback text "Volume trigger ready" → "Ready to capture"
- `data/local/datasource/DataStoreKeys.kt` — `PREMIUM_REMINDERS_ENABLED` (default false)
- `notification/PremiumReminderWorker.kt` — reads the pref via the Hilt entry point, no-ops when off
- `presentation/settings/SettingsContract.kt` + `SettingsViewModel.kt` + `SettingsScreen.kt` — "Premium feature alerts" toggle (default OFF)

## #33 Onboarding: live keyboard-widget practice

- `presentation/onboarding/OnboardingViewModel.kt` — overlay required (`mic && overlay && accessibility`); removed volume-teaching state; new flow `TapField → TapLogo → Recording → Processing → Success/Failed`; `startTutorial()` starts the real `BubbleService`; `observeImeState()` advances on a fresh IME open (rising-edge guarded)
- `presentation/onboarding/TutorialScreen.kt` — rewritten: real text field (auto-focus summons the IME + real keyboard logo), animated step copy, Tap/Speak/Done progress pills, success/failure cards, keyboard auto-hides on success and retry
- `presentation/onboarding/PermissionsScreen.kt` — "Draw over other apps" card now REQUIRED
- `presentation/onboarding/HowItWorksScreen.kt` — steps rewritten to the Tap/Speak/Done keyboard-widget flow

---

## Next step

Build to a device and run onboarding end-to-end:

```
.\gradlew.bat :app:installDebug
```

Verify: overlay-permission flow, real logo above the IME, paste into the
tutorial field, silent notification, widget nudge, hidden-apps suppression.
Then close the GitHub issues.
