package com.example.whispry.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandsFreePressResolverTest {

    private fun resolver(armingDelayMs: Long = 450L, consumeVolumeKeys: Boolean = true) =
        HandsFreePressResolver(HandsFreePressConfig(armingDelayMs, consumeVolumeKeys))

    @Test
    fun `key down from idle arms and is consumed`() {
        val t = resolver().reduce(HandsFreePressState(), HandsFreePressEvent.KeyDown)

        assertEquals(HandsFreePressPhase.ARMING, t.state.phase)
        assertTrue(t.consumed)
        assertEquals(listOf(HandsFreePressEffect.ScheduleArmingTimeout), t.effects)
    }

    @Test
    fun `holding past the arming delay starts recording`() {
        val armed = resolver().reduce(HandsFreePressState(), HandsFreePressEvent.KeyDown).state

        val t = resolver().reduce(armed, HandsFreePressEvent.ArmingTimeout)

        // Recording ownership passes to TriggerService.pressState from here — this resolver
        // only tracks the pre-recording arm/idle state, so it returns to IDLE.
        assertEquals(HandsFreePressPhase.IDLE, t.state.phase)
        assertTrue(t.consumed)
        assertEquals(listOf(HandsFreePressEffect.StartRecording), t.effects)
    }

    @Test
    fun `quick release before the delay is not consumed when consumeVolumeKeys is off`() {
        val armed = resolver().reduce(HandsFreePressState(), HandsFreePressEvent.KeyDown).state

        val t = resolver(consumeVolumeKeys = false).reduce(armed, HandsFreePressEvent.KeyUp)

        assertEquals(HandsFreePressPhase.IDLE, t.state.phase)
        assertFalse(t.consumed)
        assertEquals(listOf(HandsFreePressEffect.CancelArmingTimeout), t.effects)
    }

    @Test
    fun `quick release before the delay is still fully consumed when consumeVolumeKeys is on`() {
        val armed = resolver().reduce(HandsFreePressState(), HandsFreePressEvent.KeyDown).state

        val t = resolver(consumeVolumeKeys = true).reduce(armed, HandsFreePressEvent.KeyUp)

        assertEquals(HandsFreePressPhase.IDLE, t.state.phase)
        assertTrue(t.consumed)
    }

    @Test
    fun `stray key up from idle is a no-op and not consumed`() {
        val t = resolver().reduce(HandsFreePressState(), HandsFreePressEvent.KeyUp)

        assertEquals(HandsFreePressPhase.IDLE, t.state.phase)
        assertFalse(t.consumed)
        assertTrue(t.effects.isEmpty())
    }
}
