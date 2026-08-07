package com.example.whispry.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetGestureResolverTest {

    private val config = WidgetGestureConfig(
        armingDelayMs = 350L,
        cancelThresholdPx = 160f,
        touchSlopPx = 24f,
        doubleTapWindowMs = 300L,
        doubleTapEnabled = true
    )
    private lateinit var resolver: WidgetGestureResolver

    @Before
    fun setUp() {
        resolver = WidgetGestureResolver(config)
    }

    private fun WidgetGestureState.apply(event: WidgetGestureEvent): WidgetGestureTransition =
        resolver.reduce(this, event)

    // ------------------------------------------------------------------
    // Touch-down / arming
    // ------------------------------------------------------------------

    @Test
    fun `touch down depresses and schedules arming`() {
        val t = WidgetGestureState().apply(WidgetGestureEvent.PointerDown(1000L))

        assertEquals(WidgetGesturePhase.ARMING, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.Depress))
        assertTrue(t.effects.contains(WidgetGestureEffect.ScheduleArming))
    }

    @Test
    fun `arming completes only after the timer fires`() {
        val pressed = WidgetGestureState().apply(WidgetGestureEvent.PointerDown(1000L)).state
        assertEquals(WidgetGesturePhase.ARMING, pressed.phase)

        val t = pressed.apply(WidgetGestureEvent.ArmingTimeout(1350L))

        assertEquals(WidgetGesturePhase.RECORDING, t.state.phase)
        assertTrue(t.state.sessionActive)
        assertEquals(listOf<WidgetGestureEffect>(WidgetGestureEffect.StartRecording), t.effects)
    }

    @Test
    fun `movement beyond slop during arming aborts the press`() {
        val pressed = WidgetGestureState().apply(WidgetGestureEvent.PointerDown(1000L)).state

        val t = pressed.apply(WidgetGestureEvent.PointerMove(1100L, 0f, 40f))

        assertEquals(WidgetGesturePhase.CONSUMING, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.Release))
    }

    @Test
    fun `small movement during arming is tolerated`() {
        val pressed = WidgetGestureState().apply(WidgetGestureEvent.PointerDown(1000L)).state

        val t = pressed.apply(WidgetGestureEvent.PointerMove(1100L, 5f, -10f))

        assertEquals(WidgetGesturePhase.ARMING, t.state.phase)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun `stale arming timer after abort is ignored`() {
        val aborted = WidgetGestureState()
            .apply(WidgetGestureEvent.PointerDown(1000L)).state
            .apply(WidgetGestureEvent.PointerMove(1100L, 0f, 40f)).state

        val t = aborted.apply(WidgetGestureEvent.ArmingTimeout(1350L))

        assertEquals(WidgetGesturePhase.CONSUMING, t.state.phase)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun `stale arming timer after tap release is ignored`() {
        val released = WidgetGestureState()
            .apply(WidgetGestureEvent.PointerDown(1000L)).state
            .apply(WidgetGestureEvent.PointerUp(1100L)).state

        val t = released.apply(WidgetGestureEvent.ArmingTimeout(1350L))

        assertEquals(WidgetGesturePhase.AWAITING_SECOND_TAP, t.state.phase)
        assertTrue(t.effects.isEmpty())
    }

    // ------------------------------------------------------------------
    // Hold-to-talk: drag-down cancel + release outcomes
    // ------------------------------------------------------------------

    private fun recordingState(): WidgetGestureState =
        WidgetGestureState()
            .apply(WidgetGestureEvent.PointerDown(1000L)).state
            .apply(WidgetGestureEvent.ArmingTimeout(1350L)).state

    @Test
    fun `release while recording sends`() {
        val t = recordingState().apply(WidgetGestureEvent.PointerUp(3000L))

        assertEquals(WidgetGesturePhase.IDLE, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.SendRecording))
        assertFalse(t.effects.contains(WidgetGestureEffect.DiscardRecording))
    }

    @Test
    fun `drag below threshold does not arm cancel`() {
        val t = recordingState().apply(WidgetGestureEvent.PointerMove(2000L, 0f, 100f))

        assertFalse(t.state.cancelArmed)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun `drag past threshold arms cancel exactly once`() {
        val t = recordingState().apply(WidgetGestureEvent.PointerMove(2000L, 0f, 200f))

        assertTrue(t.state.cancelArmed)
        assertEquals(
            listOf<WidgetGestureEffect>(WidgetGestureEffect.CancelArmChanged(true)),
            t.effects
        )

        // Further movement past the threshold emits nothing new.
        val t2 = t.state.apply(WidgetGestureEvent.PointerMove(2100L, 0f, 250f))
        assertTrue(t2.state.cancelArmed)
        assertTrue(t2.effects.isEmpty())
    }

    @Test
    fun `cancel is reversible by dragging back up`() {
        val armed = recordingState().apply(WidgetGestureEvent.PointerMove(2000L, 0f, 200f)).state

        val t = armed.apply(WidgetGestureEvent.PointerMove(2100L, 0f, 80f))

        assertFalse(t.state.cancelArmed)
        assertEquals(
            listOf<WidgetGestureEffect>(WidgetGestureEffect.CancelArmChanged(false)),
            t.effects
        )

        // Releasing after re-arming upward sends.
        val t2 = t.state.apply(WidgetGestureEvent.PointerUp(2200L))
        assertTrue(t2.effects.contains(WidgetGestureEffect.SendRecording))
    }

    @Test
    fun `release while cancel armed discards`() {
        val armed = recordingState().apply(WidgetGestureEvent.PointerMove(2000L, 0f, 200f)).state

        val t = armed.apply(WidgetGestureEvent.PointerUp(2100L))

        assertEquals(WidgetGesturePhase.IDLE, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.DiscardRecording))
        assertFalse(t.effects.contains(WidgetGestureEffect.SendRecording))
    }

    @Test
    fun `session ending under the finger spends the gesture`() {
        val t = recordingState().apply(WidgetGestureEvent.SessionEnded)

        assertEquals(WidgetGesturePhase.CONSUMING, t.state.phase)
        assertFalse(t.state.sessionActive)

        // The eventual up is inert.
        val t2 = t.state.apply(WidgetGestureEvent.PointerUp(4000L))
        assertEquals(WidgetGesturePhase.IDLE, t2.state.phase)
        assertEquals(listOf<WidgetGestureEffect>(WidgetGestureEffect.Release), t2.effects)
    }

    // ------------------------------------------------------------------
    // Pointer cancellation (system stole the stream, e.g. back gesture)
    // ------------------------------------------------------------------

    @Test
    fun `cancel during arming never resolves to a tap`() {
        val pressed = WidgetGestureState().apply(WidgetGestureEvent.PointerDown(1000L)).state

        val t = pressed.apply(WidgetGestureEvent.PointerCancel)

        assertEquals(WidgetGesturePhase.IDLE, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.Release))
        assertFalse(t.effects.contains(WidgetGestureEffect.SingleTap))
        assertFalse(t.effects.contains(WidgetGestureEffect.ScheduleTapTimeout))
    }

    @Test
    fun `cancel during recording discards instead of sending`() {
        val t = recordingState().apply(WidgetGestureEvent.PointerCancel)

        assertEquals(WidgetGesturePhase.IDLE, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.DiscardRecording))
        assertFalse(t.effects.contains(WidgetGestureEffect.SendRecording))
    }

    // ------------------------------------------------------------------
    // Taps
    // ------------------------------------------------------------------

    @Test
    fun `quick release waits for a possible second tap`() {
        val t = WidgetGestureState()
            .apply(WidgetGestureEvent.PointerDown(1000L)).state
            .apply(WidgetGestureEvent.PointerUp(1100L))

        assertEquals(WidgetGesturePhase.AWAITING_SECOND_TAP, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.ScheduleTapTimeout))
        assertFalse(t.effects.contains(WidgetGestureEffect.SingleTap))
    }

    @Test
    fun `tap timeout resolves a single tap`() {
        val waiting = WidgetGestureState()
            .apply(WidgetGestureEvent.PointerDown(1000L)).state
            .apply(WidgetGestureEvent.PointerUp(1100L)).state

        val t = waiting.apply(WidgetGestureEvent.TapTimeout(1400L))

        assertEquals(WidgetGesturePhase.IDLE, t.state.phase)
        assertEquals(listOf<WidgetGestureEffect>(WidgetGestureEffect.SingleTap), t.effects)
    }

    @Test
    fun `second tap inside the window is a double tap`() {
        val waiting = WidgetGestureState()
            .apply(WidgetGestureEvent.PointerDown(1000L)).state
            .apply(WidgetGestureEvent.PointerUp(1100L)).state

        val t = waiting.apply(WidgetGestureEvent.PointerDown(1250L))

        assertEquals(WidgetGesturePhase.CONSUMING, t.state.phase)
        assertEquals(listOf<WidgetGestureEffect>(WidgetGestureEffect.DoubleTap), t.effects)
    }

    @Test
    fun `press after the window fires the pending single tap and starts fresh`() {
        val waiting = WidgetGestureState()
            .apply(WidgetGestureEvent.PointerDown(1000L)).state
            .apply(WidgetGestureEvent.PointerUp(1100L)).state

        val t = waiting.apply(WidgetGestureEvent.PointerDown(1600L))

        assertEquals(WidgetGesturePhase.ARMING, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.SingleTap))
        assertTrue(t.effects.contains(WidgetGestureEffect.ScheduleArming))
    }

    @Test
    fun `single tap fires immediately when double tap is disabled`() {
        val noDouble = WidgetGestureResolver(config.copy(doubleTapEnabled = false))
        val pressed = noDouble.reduce(
            WidgetGestureState(), WidgetGestureEvent.PointerDown(1000L)
        ).state

        val t = noDouble.reduce(pressed, WidgetGestureEvent.PointerUp(1100L))

        assertEquals(WidgetGesturePhase.IDLE, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.SingleTap))
    }

    // ------------------------------------------------------------------
    // Debounce during an active session
    // ------------------------------------------------------------------

    @Test
    fun `press during an active session is swallowed`() {
        val busy = WidgetGestureState().apply(WidgetGestureEvent.SessionStarted).state

        val t = busy.apply(WidgetGestureEvent.PointerDown(5000L))

        assertEquals(WidgetGesturePhase.CONSUMING, t.state.phase)
        assertTrue(t.effects.isEmpty())

        val t2 = t.state.apply(WidgetGestureEvent.PointerUp(5100L))
        assertEquals(WidgetGesturePhase.IDLE, t2.state.phase)
        assertFalse(t2.effects.contains(WidgetGestureEffect.SingleTap))
    }

    @Test
    fun `widget works again after the session ends`() {
        val busy = WidgetGestureState().apply(WidgetGestureEvent.SessionStarted).state
        val freed = busy.apply(WidgetGestureEvent.SessionEnded).state

        val t = freed.apply(WidgetGestureEvent.PointerDown(9000L))

        assertEquals(WidgetGesturePhase.ARMING, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.Depress))
    }

    // ------------------------------------------------------------------
    // RAMP-mode sliver: swipe-to-reveal, collapse-on-idle, collapse-after-session
    // ------------------------------------------------------------------

    private val sliverResolver = WidgetGestureResolver(
        config.copy(slimSliverEnabled = true, revealThresholdPx = 56f)
    )

    @Test
    fun `touch alone on the sliver does not arm`() {
        val t = sliverResolver.reduce(
            WidgetGestureState(phase = WidgetGesturePhase.SLIVER),
            WidgetGestureEvent.PointerDown(1000L)
        )

        assertEquals(WidgetGesturePhase.SLIVER, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.Depress))
        assertFalse(t.effects.contains(WidgetGestureEffect.ScheduleArming))
    }

    @Test
    fun `swipe past the reveal threshold reveals the full widget`() {
        val pressed = sliverResolver.reduce(
            WidgetGestureState(phase = WidgetGesturePhase.SLIVER),
            WidgetGestureEvent.PointerDown(1000L)
        ).state

        val t = sliverResolver.reduce(pressed, WidgetGestureEvent.PointerMove(1100L, 60f, 0f))

        assertEquals(WidgetGesturePhase.IDLE, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.RevealWidget))
    }

    @Test
    fun `swipe under the reveal threshold stays collapsed`() {
        val pressed = sliverResolver.reduce(
            WidgetGestureState(phase = WidgetGesturePhase.SLIVER),
            WidgetGestureEvent.PointerDown(1000L)
        ).state

        val moved = sliverResolver.reduce(pressed, WidgetGestureEvent.PointerMove(1100L, 20f, 0f)).state
        assertEquals(WidgetGesturePhase.SLIVER, moved.phase)

        val t = sliverResolver.reduce(moved, WidgetGestureEvent.PointerUp(1200L))

        assertEquals(WidgetGesturePhase.SLIVER, t.state.phase)
        assertFalse(t.effects.contains(WidgetGestureEffect.RevealWidget))
    }

    @Test
    fun `revealed widget arms hold-to-talk exactly like today's IDLE`() {
        val revealed = sliverResolver.reduce(
            sliverResolver.reduce(
                WidgetGestureState(phase = WidgetGesturePhase.SLIVER),
                WidgetGestureEvent.PointerDown(1000L)
            ).state,
            WidgetGestureEvent.PointerMove(1100L, 60f, 0f)
        ).state
        assertEquals(WidgetGesturePhase.IDLE, revealed.phase)

        val t = sliverResolver.reduce(revealed, WidgetGestureEvent.PointerDown(2000L))

        assertEquals(WidgetGesturePhase.ARMING, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.ScheduleArming))
    }

    @Test
    fun `idle timeout collapses the revealed widget back to the sliver`() {
        val t = sliverResolver.reduce(WidgetGestureState(), WidgetGestureEvent.CollapseToSliver)

        assertEquals(WidgetGesturePhase.SLIVER, t.state.phase)
        assertEquals(listOf(WidgetGestureEffect.CollapseWidget), t.effects)
    }

    @Test
    fun `collapse timer mid-gesture is a stale no-op`() {
        val arming = sliverResolver.reduce(
            WidgetGestureState(), WidgetGestureEvent.PointerDown(1000L)
        ).state
        assertEquals(WidgetGesturePhase.ARMING, arming.phase)

        val t = sliverResolver.reduce(arming, WidgetGestureEvent.CollapseToSliver)

        assertEquals(WidgetGesturePhase.ARMING, t.state.phase)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun `session ending while resting revealed collapses immediately to the sliver`() {
        val t = sliverResolver.reduce(WidgetGestureState(), WidgetGestureEvent.SessionEnded)

        assertEquals(WidgetGesturePhase.SLIVER, t.state.phase)
        assertFalse(t.state.sessionActive)
        assertTrue(t.effects.contains(WidgetGestureEffect.CollapseWidget))
    }

    @Test
    fun `CORNER-mode config (slimSliverEnabled false) never enters the sliver`() {
        val cornerResolver = WidgetGestureResolver(config) // slimSliverEnabled = false by default

        val t = cornerResolver.reduce(WidgetGestureState(), WidgetGestureEvent.SessionEnded)

        assertEquals(WidgetGesturePhase.IDLE, t.state.phase)
        assertFalse(t.effects.contains(WidgetGestureEffect.CollapseWidget))
    }

    // ------------------------------------------------------------------
    // RAMP-mode sliver: direction-aware edge swipe (reveal inward, collapse outward)
    // ------------------------------------------------------------------

    @Test
    fun `swipe the wrong way off the sliver does not reveal`() {
        val pressed = sliverResolver.reduce(
            WidgetGestureState(phase = WidgetGesturePhase.SLIVER),
            WidgetGestureEvent.PointerDown(1000L)
        ).state

        // Negative dx = toward the edge, not inward — must not reveal.
        val t = sliverResolver.reduce(pressed, WidgetGestureEvent.PointerMove(1100L, -60f, 0f))

        assertEquals(WidgetGesturePhase.SLIVER, t.state.phase)
        assertFalse(t.effects.contains(WidgetGestureEffect.RevealWidget))
    }

    @Test
    fun `a vertical drag on the sliver does not reveal`() {
        val pressed = sliverResolver.reduce(
            WidgetGestureState(phase = WidgetGesturePhase.SLIVER),
            WidgetGestureEvent.PointerDown(1000L)
        ).state

        val t = sliverResolver.reduce(pressed, WidgetGestureEvent.PointerMove(1100L, 0f, 200f))

        assertEquals(WidgetGesturePhase.SLIVER, t.state.phase)
        assertFalse(t.effects.contains(WidgetGestureEffect.RevealWidget))
    }

    @Test
    fun `swiping back toward the edge while arming collapses to the sliver`() {
        val pressed = sliverResolver.reduce(
            WidgetGestureState(), WidgetGestureEvent.PointerDown(1000L)
        ).state
        assertEquals(WidgetGesturePhase.ARMING, pressed.phase)

        val t = sliverResolver.reduce(pressed, WidgetGestureEvent.PointerMove(1100L, -60f, 0f))

        assertEquals(WidgetGesturePhase.SLIVER, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.CollapseWidget))
    }

    @Test
    fun `small jitter while arming does not collapse`() {
        val pressed = sliverResolver.reduce(
            WidgetGestureState(), WidgetGestureEvent.PointerDown(1000L)
        ).state

        val t = sliverResolver.reduce(pressed, WidgetGestureEvent.PointerMove(1100L, -10f, 0f))

        assertEquals(WidgetGesturePhase.ARMING, t.state.phase)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun `a slow push-back past slop but short of threshold keeps tracking instead of aborting`() {
        val pressed = sliverResolver.reduce(
            WidgetGestureState(), WidgetGestureEvent.PointerDown(1000L)
        ).state
        assertEquals(WidgetGesturePhase.ARMING, pressed.phase)

        // touchSlopPx=24, revealThresholdPx=56 — this used to hit the accidental-brush abort
        // before ever reaching the collapse threshold, since 24 < 56 and dx grows continuously.
        val t = sliverResolver.reduce(pressed, WidgetGestureEvent.PointerMove(1100L, -35f, 0f))

        assertEquals(WidgetGesturePhase.ARMING, t.state.phase)
        assertTrue(t.effects.contains(WidgetGestureEffect.CancelArmingTimer))
        assertFalse(t.effects.contains(WidgetGestureEffect.Release))
    }

    @Test
    fun `letting go mid push-back settles back to idle, not a tap or a hold`() {
        val pressed = sliverResolver.reduce(
            WidgetGestureState(), WidgetGestureEvent.PointerDown(1000L)
        ).state
        val dragging = sliverResolver.reduce(pressed, WidgetGestureEvent.PointerMove(1100L, -35f, 0f)).state

        val t = sliverResolver.reduce(dragging, WidgetGestureEvent.PointerUp(1200L))

        assertEquals(WidgetGesturePhase.IDLE, t.state.phase)
        assertFalse(t.effects.contains(WidgetGestureEffect.SingleTap))
        assertFalse(t.effects.contains(WidgetGestureEffect.ScheduleTapTimeout))
    }

    @Test
    fun `CORNER-mode config never collapses on swipe since it has no sliver`() {
        val cornerResolver = WidgetGestureResolver(config) // slimSliverEnabled = false by default
        val pressed = cornerResolver.reduce(
            WidgetGestureState(), WidgetGestureEvent.PointerDown(1000L)
        ).state

        // A big swipe would collapse in RAMP mode, but CORNER mode has no sliver to collapse to —
        // it must fall back to the ordinary accidental-brush abort.
        val t = cornerResolver.reduce(pressed, WidgetGestureEvent.PointerMove(1100L, -60f, 0f))

        assertEquals(WidgetGesturePhase.CONSUMING, t.state.phase)
        assertFalse(t.effects.contains(WidgetGestureEffect.CollapseWidget))
    }
}
