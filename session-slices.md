# Session Slices — Responsive Screens

> Implemented slice by slice. Mark `[x]` when done.

---

## Slice 0: Foundation — WindowWidthSizeClass + Adaptive Nav
- [x] Replace `currentDeviceType()` with `WindowWidthSizeClass` enum (compact/medium/expanded) — no library needed, just `screenWidthDp` comparison
- [x] Merge `PhoneLayout` / `TabletLayout` into unified `AdaptiveLayout` that reads width class + orientation
- [x] Bottom tabs for Compact width + portrait; right-side rail for all other configs
- [x] Rail: full-height on tablet, content-height centered on phone landscape
- [x] Content area uses `PaddingValues` so items don't overlap rail
- [x] Delete old `currentDeviceType()` — no callers remain

ponytail: skipped `material3-adaptive` dep — `WindowSizeClass` is a 3-way `when` on `screenWidthDp`, not worth a library.

---

## Slice 1: HomeScreen Adaptive Layout
- [ ] Compact: existing single-column (160dp mic, stacked stats)
- [ ] Medium (phone landscape): 96dp mic, stats row inline with mic, tighter vertical spacing
- [ ] Expanded (tablet): 120dp mic, 2-column recent transcripts grid

---

## Slice 2: HistoryScreen Layout
- [ ] Header/search bar full-width with standard `screen_horizontal_padding`
- [ ] Edge-to-edge fade gradient behind nav rail
- [ ] 2-column `LazyVerticalGrid` for Expanded width
- [ ] Content padding accounts for nav rail width

---

## Slice 3: DetailScaffold + Feature Screens
- [ ] Extract `DetailScaffold` composable (back-button + title + subtitle + scrollable content, adaptive padding)
- [ ] Apply to `HistoryDetailScreen`
- [ ] Apply to `TextExpanderScreen`
- [ ] Apply to `AppToneScreen`
- [ ] Apply to `MemoryScreen`

---

## Slice 4: Remaining Screens (Settings, Presets, Permissions)
- [ ] Settings: `Modifier.widthIn(max = 600.dp)` for Expanded
- [ ] Presets: `GridCells.Adaptive(minSize = 200.dp)` replacing fixed(2)
- [ ] PermissionsScreen: permission cards + button adapt for landscape/tablet

---

## Slice 5: Overlays/Dialogs Max-Width
- [ ] `TranscriptDetailView`: 480dp max on Medium/Expanded
- [ ] `ExportBottomSheet`: 480dp max
- [ ] `PresetPickerBottomSheet`: 480dp max
- [ ] `AddExpanderDialog`, `AddMemoryDialog`, `AddAppToneDialog`: 480dp max
- [ ] `FilterMenu`: 480dp max
