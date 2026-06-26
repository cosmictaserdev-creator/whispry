# PRD: Bubble Adaptive Layout for Landscape & Tablet

## Problem Statement

Whispry's floating bubble and expanded panel are currently designed and tested only in portrait, phone-width contexts. On landscape orientation and on tablets, this breaks down in several ways:

- The bubble's resting position is computed against portrait screen bounds. On rotation or on a tablet's larger canvas, it can end up off-screen, awkwardly placed, or snapped to the wrong edge.
- The bubble pill — designed for narrow portrait width — stretches uncomfortably wide on a tablet.
- Window insets, keyboard insets, and safe-area handling for the overlay have not been verified outside portrait phone testing.

## Solution

The bubble will adapt to any screen size and orientation through three changes:

1. **Fluid pill width** — the bubble pill caps its width to a readable maximum rather than stretching endlessly on wide screens.
2. **Rotation-safe positioning** — bubble position is stored as a percentage of safe bounds rather than raw pixels, and re-snapped on configuration change.
3. **Smart snapping with orientation** — the bubble snaps to one of three targets depending on where the user releases it: center-top (horizontal pill), center-bottom (horizontal pill), or left/right edge (vertical strip). The record/stop button always sits at the bottom.

The floating widget panel is simplified (no transcript, solid background) and shares its positioning logic with the bubble.

## User Stories

1. As a user, I want the bubble pill to never exceed a comfortable width on any screen, so that it always looks proportionate on both phones and tablets.
2. As a user, I want the bubble to stay on-screen after rotating my device, so that I don't lose access to it.
3. As a user, I want the bubble to remember its general position after rotation, so that I don't have to reposition it every time I rotate.
4. As a user on a tablet, I want the bubble to look appropriately sized on my larger screen, so that it doesn't appear tiny or lost.
5. As a user, I want the bubble to snap to center-bottom when I release it near the bottom center, so that it sits conveniently for thumb access.
6. As a user, I want the bubble to snap to center-top when I release it near the top center, so that it sits conveniently out of the way.
7. As a user, I want the bubble to snap to the nearest edge when I drag it away from center, so that it stays out of my content.
8. As a user, I want the bubble to switch between a horizontal pill at top/bottom center and a vertical strip along the edge, so that it takes up minimal space in each position.
9. As a user, I want the record/stop button to always be at the bottom of the bubble regardless of orientation or snap position, so that it's easy to reach.
10. As a user, I want the floating widget to have a solid background, so that it's more readable against varied app content.
11. As a developer, I want the positioning and snapping logic extracted into a testable pure Kotlin class, so that I can verify edge cases without an emulator.
12. As a user, I want the floating widget to retain its simplified appearance (no transcript, just header + action chips), so that I can quickly access actions without clutter.

## Implementation Decisions

### New: `BubblePositionManager`

A pure Kotlin class extracted from the positioning/snapping code currently embedded in `BubbleService` and `FloatingWidgetManager`. It has zero Android framework dependencies — it takes screen dimensions as input and returns computed positions.

**Interface shape:**

```
fun snapTarget(currentPosition: Offset, bubbleSize: Size): SnapTarget
fun normalize(position: Offset, safeBounds: Rect): Pair<Float, Float>
fun denormalize(normalizedX: Float, normalizedY: Float, safeBounds: Rect): Offset
fun orientationFor(snapTarget: SnapTarget): BubbleOrientation
fun defaultPosition(safeBounds: Rect, bubbleSize: Size): Offset
```

**Snap targets:** `CenterTop`, `CenterBottom`, `LeftEdge`, `RightEdge`

**Snap rules:**
- If `currentPosition.x` is within ±100dp of horizontal center → snap to `CenterTop` if in top half of screen, `CenterBottom` if in bottom half
- Otherwise → snap to nearest left or right edge at the current Y position
- Orientation is `Horizontal` for center targets, `Vertical` for edge targets
- Record/stop button always positioned at the bottom of the pill/strip

### Bubble pill width

Replace fixed dp widths with a fluid constraint:

```
Modifier.fillMaxWidth(0.85f).widthIn(max = 420.dp)
```

Applied to the pill's outer `Box` in `BubbleOverlay.kt`. The specific minimum widths for each state (56.dp, 200.dp, etc.) are preserved as minimums — the pill will be at least that wide but will never exceed 85% of screen width or 420dp.

### Position storage

Continue using the existing `BUBBLE_POSITION_X` / `BUBBLE_POSITION_Y` DataStore keys, but now store values as **integer percentages** (0–100) of safe bounds width/height instead of raw pixel values. On restore, denormalize using current safe bounds.

### Configuration change handling

- Register a `onConfigurationChanged` callback or listen for `Configuration` changes in `BubbleService`
- On change: read current safe bounds → denormalize saved position → call `snapTarget()` → animate to new position
- The `BubbleService` foreground service persists across config changes, so the ComposeView just needs to be repositioned

### Floating widget changes

- Remove transcript display from `WidgetUI`
- Swap glass backdrop for a solid background color
- Remove the width constraint (wrap_content is sufficient without transcript text)
- Reuse `BubblePositionManager` for drag/snap/logic

### No keyboard inset changes

Keyboard insets are out of scope for this PRD. The `TYPE_APPLICATION_OVERLAY` window type floats above most surfaces by default. After implementation, test keyboard scenarios and log any issues — no code changes unless a real problem is found.

## Testing Decisions

### What makes a good test

Test only external behavior: given screen dimensions, bubble size, and a current position, verify the correct snap target and computed position. Do not test internal state transitions or animation timing.

### Seam

The primary test seam is `BubblePositionManager` — a pure Kotlin class with no Android framework dependencies. It receives screen bounds and positions as value types and returns snap decisions as sealed class instances. This is the highest seam possible; no `WindowManager`, `ComposeView`, or `ValueAnimator` is needed.

### Module to test

`BubblePositionManager` lives in the `service` package (alongside the other overlay code). Unit tests go in `app/src/test/java/com/example/whispry/service/BubblePositionManagerTest.kt`.

### Prior art

- `MFCCExtractorTest` — pure function tests with JUnit4, `assertEquals`, known-inputs-known-outputs pattern
- `AppToneViewModelTest` — MockK + `StandardTestDispatcher` / `runTest`
- The existing codebase does not use Compose UI tests (`ui-test-junit4` is declared in the catalog but not wired into build). Compose UI test setup is out of scope.

### Test scenarios

- Given a phone-portrait safe bounds rect, bubble at center-bottom zone → returns `CenterBottom` with horizontal orientation
- Given same bounds, bubble at center-top zone → returns `CenterTop` with horizontal orientation
- Given same bounds, bubble far right → returns `RightEdge` with vertical orientation
- Given tablet-landscape bounds, bubble at center → returns `CenterBottom` with horizontal orientation
- Normalize a position to % and denormalize back → identity
- Edge cases: bubble at exact center boundary (±100dp threshold), bubble at screen corners, bubble partially off-screen

## Out of Scope

- Keyboard insets handling (verify only, no code fix)
- Compose UI test infrastructure setup (`ui-test-junit4` wiring, `createComposeRule`)
- Side-by-side layout for the widget panel
- Transcript display in the widget panel (removed by design)
- Unifying the bubble and widget into a single component
- Full tablet navigation redesign (already handled by `MainScreen`/`TabletLayout`)
- Widget width cap (wrap_content is sufficient without transcript)
- Any changes to main app screens (`HomeScreen`, `HistoryScreen`, `SettingsScreen`)
- Animation timing or visual polish beyond the snap mechanics
- Accessibility auditing

## Further Notes

- The `WindowOverlayCoordinator` already manages which overlay (Bubble vs Widget) is visible — no changes needed there.
- The existing `WindowSize.kt` / `currentDeviceType()` composable is not used by this work; the sizing is purely based on safe bounds, not device classification.
- The pre-API-30 fallback in `getSafeWindowBounds()` is preserved but not improved — the real fix is the percentage-based positioning which works regardless of the bounds accuracy.
