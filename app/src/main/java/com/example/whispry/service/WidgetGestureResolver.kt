// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import kotlin.math.abs

/**
 * Pure reducer for the floating widget's gesture contract:
 *
 *   touch-down -> depress (+tick) -> arming delay -> hold-to-talk recording
 *   drag-down while recording -> cancel-armed (reversible) -> release = discard
 *   release above threshold = send; quick release before arming = tap
 *   second tap within the window = double tap; presses during a session = debounced
 *
 * No Android dependencies, no clocks, no coroutines — time arrives inside events,
 * timers are requested via [WidgetGestureEffect.ScheduleArming] /
 * [WidgetGestureEffect.ScheduleTapTimeout] and delivered back as events, so the
 * whole contract is testable as plain JVM unit tests.
 */
data class WidgetGestureConfig(
    /** Anti-accident delay between touch-down and recording actually arming. */
    val armingDelayMs: Long = 350L,
    /** How far (px) the finger must travel downward while recording to arm cancel. */
    val cancelThresholdPx: Float = 160f,
    /** Movement beyond this (px) during arming aborts the press as an accidental brush. */
    val touchSlopPx: Float = 24f,
    /** Window (ms) after a tap release in which a second tap counts as a double tap. */
    val doubleTapWindowMs: Long = 300L,
    /** When false, single taps fire immediately on release (no double-tap wait). */
    val doubleTapEnabled: Boolean = true,
    /**
     * RAMP-mode edge sliver: when true, the resting phase is [WidgetGesturePhase.SLIVER] rather
     * than [WidgetGesturePhase.IDLE] — a swipe past [revealThresholdPx] is required to reveal the
     * full-size widget (IDLE) before hold-to-talk/tap gestures apply. False for CORNER mode,
     * which keeps today's touch-arms-directly behavior entirely (SLIVER is never entered).
     */
    val slimSliverEnabled: Boolean = false,
    /** How far (px) a swipe from the sliver must travel to reveal the full widget. */
    val revealThresholdPx: Float = 56f
)

enum class WidgetGesturePhase {
    /** No finger down, nothing pending. */
    IDLE,

    /** Finger down, waiting out the arming delay. */
    ARMING,

    /** Armed: hold-to-talk recording is running, tracking drag-down cancel. */
    RECORDING,

    /** A quick tap was released; waiting to see if a second tap lands. */
    AWAITING_SECOND_TAP,

    /** Finger is down but the gesture is spent (debounced / aborted); ignore until up. */
    CONSUMING,

    /** RAMP-mode collapsed edge sliver (only reachable when [WidgetGestureConfig.slimSliverEnabled]):
     *  a swipe is required to reveal the full widget (IDLE); a touch alone does not arm. */
    SLIVER
}

data class WidgetGestureState(
    val phase: WidgetGesturePhase = WidgetGesturePhase.IDLE,
    val pressedAtMs: Long = 0L,
    val dragY: Float = 0f,
    val cancelArmed: Boolean = false,
    /** True while a recording session is live (started by this widget or any other trigger). */
    val sessionActive: Boolean = false,
    /** Cumulative swipe distance (px) tracked while attempting to reveal from SLIVER. */
    val revealDrag: Float = 0f
)

sealed interface WidgetGestureEvent {
    data class PointerDown(val timeMs: Long) : WidgetGestureEvent

    /** Cumulative offset from the down position. */
    data class PointerMove(val timeMs: Long, val dx: Float, val dy: Float) : WidgetGestureEvent
    data class PointerUp(val timeMs: Long) : WidgetGestureEvent

    /**
     * The system stole the pointer stream (back-gesture swipe, window change) — the press
     * must die without ever resolving to a tap or a send.
     */
    data object PointerCancel : WidgetGestureEvent

    /** The arming timer requested via [WidgetGestureEffect.ScheduleArming] fired. */
    data class ArmingTimeout(val timeMs: Long) : WidgetGestureEvent

    /** The double-tap timer requested via [WidgetGestureEffect.ScheduleTapTimeout] fired. */
    data class TapTimeout(val timeMs: Long) : WidgetGestureEvent

    /** A recording session started (any trigger). */
    data object SessionStarted : WidgetGestureEvent

    /** The recording session ended (sent, discarded, or errored). */
    data object SessionEnded : WidgetGestureEvent

    /** The idle timer (RAMP + slimSliverEnabled only) requests collapsing back to the sliver. */
    data object CollapseToSliver : WidgetGestureEvent
}

sealed interface WidgetGestureEffect {
    /** Visually depress the switch + light tick haptic. */
    data object Depress : WidgetGestureEffect

    /** Restore the un-pressed look (gesture over or aborted). */
    data object Release : WidgetGestureEffect

    /** Start the arming timer for [WidgetGestureConfig.armingDelayMs]. */
    data object ScheduleArming : WidgetGestureEffect

    /** Start the double-tap timer for [WidgetGestureConfig.doubleTapWindowMs]. */
    data object ScheduleTapTimeout : WidgetGestureEffect

    /** Arming completed: firm click haptic + start hold-to-talk recording. */
    data object StartRecording : WidgetGestureEffect

    /** Released above the cancel threshold: stop recording and send. */
    data object SendRecording : WidgetGestureEffect

    /** Released below the cancel threshold: discard the recording. */
    data object DiscardRecording : WidgetGestureEffect

    /** Drag crossed (or un-crossed) the cancel threshold; drives the red pill mirror + warn haptic. */
    data class CancelArmChanged(val armed: Boolean) : WidgetGestureEffect

    /** A single tap resolved (either immediately or after the double-tap window lapsed). */
    data object SingleTap : WidgetGestureEffect

    /** A second tap landed inside the double-tap window. */
    data object DoubleTap : WidgetGestureEffect

    /** A swipe from the sliver crossed [WidgetGestureConfig.revealThresholdPx]: show the full widget. */
    data object RevealWidget : WidgetGestureEffect

    /** Collapse the full widget back to the slim edge sliver. */
    data object CollapseWidget : WidgetGestureEffect

    /** A push-back drag is in progress: stop the arming timer so it can't race a slow,
     *  deliberate collapse swipe into starting a recording instead. */
    data object CancelArmingTimer : WidgetGestureEffect
}

data class WidgetGestureTransition(
    val state: WidgetGestureState,
    val effects: List<WidgetGestureEffect> = emptyList()
)

class WidgetGestureResolver(private val config: WidgetGestureConfig) {

    fun reduce(state: WidgetGestureState, event: WidgetGestureEvent): WidgetGestureTransition =
        when (event) {
            is WidgetGestureEvent.PointerDown -> onDown(state, event.timeMs)
            is WidgetGestureEvent.PointerMove -> onMove(state, event.dx, event.dy)
            is WidgetGestureEvent.PointerUp -> onUp(state, event.timeMs)
            WidgetGestureEvent.PointerCancel -> onPointerCancel(state)
            is WidgetGestureEvent.ArmingTimeout -> onArmingTimeout(state)
            is WidgetGestureEvent.TapTimeout -> onTapTimeout(state)
            WidgetGestureEvent.SessionStarted ->
                WidgetGestureTransition(state.copy(sessionActive = true))
            WidgetGestureEvent.SessionEnded -> onSessionEnded(state)
            WidgetGestureEvent.CollapseToSliver -> onCollapseToSliver(state)
        }

    private fun onDown(state: WidgetGestureState, timeMs: Long): WidgetGestureTransition =
        when (state.phase) {
            WidgetGesturePhase.IDLE -> {
                if (state.sessionActive) {
                    // Debounce: a session is already running; swallow this press entirely.
                    WidgetGestureTransition(state.copy(phase = WidgetGesturePhase.CONSUMING))
                } else {
                    WidgetGestureTransition(
                        state.copy(
                            phase = WidgetGesturePhase.ARMING,
                            pressedAtMs = timeMs,
                            dragY = 0f,
                            cancelArmed = false,
                            revealDrag = 0f
                        ),
                        listOf(WidgetGestureEffect.Depress, WidgetGestureEffect.ScheduleArming)
                    )
                }
            }
            WidgetGesturePhase.AWAITING_SECOND_TAP -> {
                if (state.sessionActive) {
                    WidgetGestureTransition(state.copy(phase = WidgetGesturePhase.CONSUMING))
                } else if (timeMs - state.pressedAtMs <= config.doubleTapWindowMs) {
                    WidgetGestureTransition(
                        state.copy(phase = WidgetGesturePhase.CONSUMING),
                        listOf(WidgetGestureEffect.DoubleTap)
                    )
                } else {
                    // Window expired but the timeout event hasn't landed yet: resolve the
                    // pending single tap, then treat this press as a fresh gesture.
                    WidgetGestureTransition(
                        state.copy(
                            phase = WidgetGesturePhase.ARMING,
                            pressedAtMs = timeMs,
                            dragY = 0f,
                            cancelArmed = false
                        ),
                        listOf(
                            WidgetGestureEffect.SingleTap,
                            WidgetGestureEffect.Depress,
                            WidgetGestureEffect.ScheduleArming
                        )
                    )
                }
            }
            WidgetGesturePhase.SLIVER -> {
                if (state.sessionActive) {
                    WidgetGestureTransition(state.copy(phase = WidgetGesturePhase.CONSUMING))
                } else {
                    // Touch alone never arms from the sliver — only a swipe (tracked in onMove)
                    // reveals the full widget.
                    WidgetGestureTransition(
                        state.copy(revealDrag = 0f),
                        listOf(WidgetGestureEffect.Depress)
                    )
                }
            }
            // A second pointer while one is already down: ignore.
            else -> WidgetGestureTransition(state)
        }

    private fun onMove(state: WidgetGestureState, dx: Float, dy: Float): WidgetGestureTransition =
        when (state.phase) {
            WidgetGesturePhase.ARMING -> {
                if (config.slimSliverEnabled && dx <= -config.revealThresholdPx) {
                    // Deliberate swipe back toward the anchored edge: collapse to the sliver
                    // instead of treating it as an accidental brush that aborts the press.
                    WidgetGestureTransition(
                        state.copy(phase = WidgetGesturePhase.SLIVER, revealDrag = 0f),
                        listOf(WidgetGestureEffect.Release, WidgetGestureEffect.CollapseWidget)
                    )
                } else if (config.slimSliverEnabled && dx < -config.touchSlopPx) {
                    // Outward drag past the accidental-brush slop but short of the collapse
                    // threshold: a deliberate (if slow) push-back in progress. touchSlopPx is
                    // smaller than revealThresholdPx, so without this branch every push-back
                    // hit the brush-abort case below before it could ever reach the collapse
                    // check above — collapsing by drag was unreachable. Keep tracking instead
                    // of aborting, and hold off the arming timer so a slow push doesn't get
                    // raced into starting a recording out from under the user's thumb.
                    WidgetGestureTransition(
                        state.copy(revealDrag = -dx),
                        listOf(WidgetGestureEffect.CancelArmingTimer)
                    )
                } else if (abs(dx) > config.touchSlopPx || abs(dy) > config.touchSlopPx) {
                    // Accidental brush: abort the press before it arms.
                    WidgetGestureTransition(
                        state.copy(phase = WidgetGesturePhase.CONSUMING),
                        listOf(WidgetGestureEffect.Release)
                    )
                } else {
                    WidgetGestureTransition(state)
                }
            }
            WidgetGesturePhase.RECORDING -> {
                val armed = dy >= config.cancelThresholdPx
                val effects = if (armed != state.cancelArmed) {
                    listOf(WidgetGestureEffect.CancelArmChanged(armed))
                } else {
                    emptyList()
                }
                WidgetGestureTransition(
                    state.copy(dragY = dy, cancelArmed = armed),
                    effects
                )
            }
            WidgetGesturePhase.SLIVER -> {
                // Direction-aware: only a swipe from the edge toward screen center (positive,
                // inward dx) reveals — a swipe the other way, or a vertical drag, does not.
                if (dx >= config.revealThresholdPx) {
                    WidgetGestureTransition(
                        state.copy(phase = WidgetGesturePhase.IDLE, revealDrag = 0f),
                        listOf(WidgetGestureEffect.Release, WidgetGestureEffect.RevealWidget)
                    )
                } else {
                    WidgetGestureTransition(state.copy(revealDrag = dx.coerceAtLeast(0f)))
                }
            }
            else -> WidgetGestureTransition(state)
        }

    private fun onUp(state: WidgetGestureState, timeMs: Long): WidgetGestureTransition =
        when (state.phase) {
            WidgetGesturePhase.ARMING -> {
                if (config.slimSliverEnabled && state.revealDrag > config.touchSlopPx) {
                    // Let go mid push-back, before it crossed the collapse threshold: not a
                    // tap and not a hold, just settle back to resting revealed.
                    WidgetGestureTransition(
                        state.copy(phase = WidgetGesturePhase.IDLE, revealDrag = 0f),
                        listOf(WidgetGestureEffect.Release)
                    )
                }
                // Released before arming completed: this is a tap.
                else if (config.doubleTapEnabled) {
                    WidgetGestureTransition(
                        state.copy(
                            phase = WidgetGesturePhase.AWAITING_SECOND_TAP,
                            pressedAtMs = timeMs
                        ),
                        listOf(WidgetGestureEffect.Release, WidgetGestureEffect.ScheduleTapTimeout)
                    )
                } else {
                    WidgetGestureTransition(
                        state.copy(phase = WidgetGesturePhase.IDLE),
                        listOf(WidgetGestureEffect.Release, WidgetGestureEffect.SingleTap)
                    )
                }
            }
            WidgetGesturePhase.RECORDING -> {
                val outcome = if (state.cancelArmed) {
                    WidgetGestureEffect.DiscardRecording
                } else {
                    WidgetGestureEffect.SendRecording
                }
                WidgetGestureTransition(
                    state.copy(
                        phase = WidgetGesturePhase.IDLE,
                        dragY = 0f,
                        cancelArmed = false
                    ),
                    listOf(WidgetGestureEffect.Release, outcome)
                )
            }
            WidgetGesturePhase.CONSUMING -> WidgetGestureTransition(
                state.copy(phase = WidgetGesturePhase.IDLE, dragY = 0f, cancelArmed = false),
                listOf(WidgetGestureEffect.Release)
            )
            WidgetGesturePhase.SLIVER -> {
                // Swipe never cleared the reveal threshold: snap back, stay collapsed.
                WidgetGestureTransition(
                    state.copy(revealDrag = 0f),
                    listOf(WidgetGestureEffect.Release)
                )
            }
            else -> WidgetGestureTransition(state)
        }

    private fun onArmingTimeout(state: WidgetGestureState): WidgetGestureTransition =
        if (state.phase == WidgetGesturePhase.ARMING) {
            WidgetGestureTransition(
                state.copy(
                    phase = WidgetGesturePhase.RECORDING,
                    dragY = 0f,
                    cancelArmed = false,
                    sessionActive = true
                ),
                listOf(WidgetGestureEffect.StartRecording)
            )
        } else {
            // Stale timer (press was aborted or released first): ignore.
            WidgetGestureTransition(state)
        }

    private fun onTapTimeout(state: WidgetGestureState): WidgetGestureTransition =
        if (state.phase == WidgetGesturePhase.AWAITING_SECOND_TAP) {
            WidgetGestureTransition(
                state.copy(phase = WidgetGesturePhase.IDLE),
                listOf(WidgetGestureEffect.SingleTap)
            )
        } else {
            WidgetGestureTransition(state)
        }

    private fun onPointerCancel(state: WidgetGestureState): WidgetGestureTransition =
        when (state.phase) {
            // Press died before arming (typically a back-gesture graze): no tap, no flash.
            WidgetGesturePhase.ARMING -> WidgetGestureTransition(
                state.copy(phase = WidgetGesturePhase.IDLE),
                listOf(WidgetGestureEffect.Release)
            )
            // Pointer stolen mid-recording: the hold is broken, so discard.
            WidgetGesturePhase.RECORDING -> WidgetGestureTransition(
                state.copy(
                    phase = WidgetGesturePhase.IDLE,
                    dragY = 0f,
                    cancelArmed = false
                ),
                listOf(WidgetGestureEffect.Release, WidgetGestureEffect.DiscardRecording)
            )
            WidgetGesturePhase.CONSUMING -> WidgetGestureTransition(
                state.copy(phase = WidgetGesturePhase.IDLE, dragY = 0f, cancelArmed = false),
                listOf(WidgetGestureEffect.Release)
            )
            WidgetGesturePhase.SLIVER -> WidgetGestureTransition(
                state.copy(revealDrag = 0f),
                listOf(WidgetGestureEffect.Release)
            )
            else -> WidgetGestureTransition(state)
        }

    private fun onSessionEnded(state: WidgetGestureState): WidgetGestureTransition =
        when (state.phase) {
            // Session died under our finger (error, external stop): spend the gesture.
            WidgetGesturePhase.RECORDING -> WidgetGestureTransition(
                state.copy(
                    phase = WidgetGesturePhase.CONSUMING,
                    sessionActive = false,
                    dragY = 0f,
                    cancelArmed = false
                ),
                listOf(WidgetGestureEffect.Release)
            )
            // Finger is already off (the common case: send/discard already resolved via onUp) and
            // resting revealed — collapse straight back to the sliver rather than waiting for the
            // idle timer, per the "collapse right after a session ends" rule.
            WidgetGesturePhase.IDLE -> if (config.slimSliverEnabled) {
                WidgetGestureTransition(
                    state.copy(phase = WidgetGesturePhase.SLIVER, sessionActive = false, revealDrag = 0f),
                    listOf(WidgetGestureEffect.CollapseWidget)
                )
            } else {
                WidgetGestureTransition(state.copy(sessionActive = false))
            }
            else -> WidgetGestureTransition(state.copy(sessionActive = false))
        }

    private fun onCollapseToSliver(state: WidgetGestureState): WidgetGestureTransition =
        if (state.phase == WidgetGesturePhase.IDLE) {
            WidgetGestureTransition(
                state.copy(phase = WidgetGesturePhase.SLIVER, revealDrag = 0f),
                listOf(WidgetGestureEffect.CollapseWidget)
            )
        } else {
            // Not resting revealed (mid-gesture, already collapsed, etc.): stale/inapplicable timer.
            WidgetGestureTransition(state)
        }
}
