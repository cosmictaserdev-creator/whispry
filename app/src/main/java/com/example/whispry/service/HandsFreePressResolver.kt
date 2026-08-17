// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

/**
 * Pure decision logic for hands-free single-press mode: press-and-hold arms a recording after
 * [HandsFreePressConfig.armingDelayMs], release before that fires is treated as a normal key
 * press. Mirrors [WidgetGestureResolver]'s reducer shape so the arming delay is testable without
 * Android's Handler/KeyEvent.
 *
 * Only tracks the pre-recording arm/idle state — once a recording actually starts, `pressState`
 * in [TriggerService] becomes the sole source of truth (it must, since starting can also fail,
 * e.g. smart-suppression), so there is no RECORDING phase here to desync against that.
 */
enum class HandsFreePressPhase { IDLE, ARMING }

data class HandsFreePressState(val phase: HandsFreePressPhase = HandsFreePressPhase.IDLE)

data class HandsFreePressConfig(
    val armingDelayMs: Long,
    /** When true, the volume key is always fully swallowed regardless of press length. */
    val consumeVolumeKeys: Boolean
)

sealed interface HandsFreePressEvent {
    data object KeyDown : HandsFreePressEvent
    data object KeyUp : HandsFreePressEvent
    data object ArmingTimeout : HandsFreePressEvent
}

sealed interface HandsFreePressEffect {
    data object StartRecording : HandsFreePressEffect
    data object ScheduleArmingTimeout : HandsFreePressEffect
    data object CancelArmingTimeout : HandsFreePressEffect
}

data class HandsFreePressTransition(
    val state: HandsFreePressState,
    val effects: List<HandsFreePressEffect> = emptyList(),
    /** Whether this key event should be reported consumed (true) or passed through to the OS. */
    val consumed: Boolean
)

class HandsFreePressResolver(private val config: HandsFreePressConfig) {

    fun reduce(state: HandsFreePressState, event: HandsFreePressEvent): HandsFreePressTransition =
        when (state.phase) {
            HandsFreePressPhase.IDLE -> onIdle(event)
            HandsFreePressPhase.ARMING -> onArming(event)
        }

    private fun onIdle(event: HandsFreePressEvent): HandsFreePressTransition = when (event) {
        HandsFreePressEvent.KeyDown -> HandsFreePressTransition(
            state = HandsFreePressState(HandsFreePressPhase.ARMING),
            effects = listOf(HandsFreePressEffect.ScheduleArmingTimeout),
            consumed = true
        )
        else -> HandsFreePressTransition(HandsFreePressState(HandsFreePressPhase.IDLE), consumed = false)
    }

    private fun onArming(event: HandsFreePressEvent): HandsFreePressTransition = when (event) {
        HandsFreePressEvent.KeyUp -> HandsFreePressTransition(
            state = HandsFreePressState(HandsFreePressPhase.IDLE),
            effects = listOf(HandsFreePressEffect.CancelArmingTimeout),
            // Quick tap under the delay: never a recording start. Only reported as consumed if
            // the user wants the key fully swallowed no matter what.
            consumed = config.consumeVolumeKeys
        )
        HandsFreePressEvent.ArmingTimeout -> HandsFreePressTransition(
            state = HandsFreePressState(HandsFreePressPhase.IDLE),
            effects = listOf(HandsFreePressEffect.StartRecording),
            consumed = true
        )
        HandsFreePressEvent.KeyDown -> HandsFreePressTransition(
            HandsFreePressState(HandsFreePressPhase.ARMING),
            consumed = true
        )
    }
}
