# Vertical Slices — Responsive Main App Screens

Parent: https://github.com/cosmictaserdev-creator/whispry/issues/7

## 1. Prefactor: WindowSizeClass infrastructure

- Add Material3 adaptive dependency to `build.gradle.kts`
- Create `WindowSizeClass` utility (replacing the binary `currentDeviceType()` approach)
- Create test helper to override `WindowSizeClass` in previews/tests
- **Blocked by:** None

## 2. Unified adaptive nav shell

- Merge `PhoneLayout` + `TabletLayout` into single `AdaptiveLayout` in `MainScreen.kt`
- Bottom tabs on Compact portrait, right-side rail on everything else
- Landscape rail is content-height (not full-height), vertically centered
- **Blocked by:** 1

## 3. HomeScreen responsive

- Three layout branches (Compact/Medium/Expanded)
- Adaptive mic size, stats row positioning, transcript grid
- **Blocked by:** 1, 2

## 4. HistoryScreen responsive

- Full-width header (no 74dp inset), edge-to-edge gradient, transcript grid on Expanded
- **Blocked by:** 1, 2

## 5. Settings + Presets screens

- Settings: single-column, `widthIn(max = 600.dp)` on Expanded
- Presets: `GridCells.Fixed(2)` → `GridCells.Adaptive(200.dp)`
- **Blocked by:** 1

## 6. DetailScaffold + detail screens

- Extract reusable `DetailScaffold` (back-button, title, subtitle, scrollable content)
- Apply to HistoryDetail, TextExpander, AppTone, Memory screens
- **Blocked by:** 1

## 7. Dialogs and overlays max-width constraint

- Constrain all shared overlays to `maxWidth = 480.dp` on Medium+Expanded
- Covers: TranscriptDetailView, ExportBottomSheet, PresetPickerBottomSheet, AddExpander/Memory/AppTone dialogs, FilterMenu
- **Blocked by:** 1

## 8. Onboarding screens responsive

- PermissionsScreen: card layout + continue button adapts
- Other onboarding screens: padding/alignment pass
- **Blocked by:** 1
