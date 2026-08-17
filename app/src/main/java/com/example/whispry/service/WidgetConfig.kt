// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import androidx.datastore.preferences.core.Preferences
import com.example.whispry.data.local.datasource.DataStoreKeys

/** Manual override for the springy animations: AUTO follows the OS "remove animations" setting. */
enum class WidgetMotionSetting {
    AUTO, ON, OFF;

    companion object {
        fun fromName(name: String?): WidgetMotionSetting =
            entries.find { it.name == name } ?: AUTO
    }
}

/**
 * What a widget tap performs. Hold is always hold-to-talk; single/double tap are configurable.
 * Serialized as: "NONE", "TOGGLE", or "TOGGLE:" + a [com.example.whispry.domain.model.PressAction]
 * string for preset/open-app variants.
 */
sealed interface WidgetTapAction {
    /** Tap does nothing. */
    data object None : WidgetTapAction

    /** Tap starts a recording (tap again to stop), routed through [pressAction]. */
    data class ToggleRecord(val pressAction: String = "NORMAL") : WidgetTapAction

    fun serialize(): String = when (this) {
        None -> "NONE"
        is ToggleRecord -> if (pressAction == "NORMAL") "TOGGLE" else "TOGGLE:$pressAction"
    }

    companion object {
        fun parse(raw: String?): WidgetTapAction = when {
            raw.isNullOrBlank() || raw == "NONE" -> None
            raw == "TOGGLE" -> ToggleRecord()
            raw.startsWith("TOGGLE:") -> ToggleRecord(raw.removePrefix("TOGGLE:"))
            else -> None
        }
    }
}

/**
 * Full persisted configuration of the floating widget, snapshotted from DataStore.
 * Defaults follow the PRD: ramp mode on the right edge, 72dp inner face, 26dp
 * protrusion, 4s fade to 40% opacity, 350ms anti-accident arming delay.
 */
data class WidgetConfig(
    val enabled: Boolean = DataStoreKeys.DEFAULT_FLOATING_WIDGET_ENABLED,
    val baseHeightDp: Int = 72,
    val protrusionDp: Int = 26,
    val edgeClearanceDp: Int = DataStoreKeys.DEFAULT_WIDGET_EDGE_CLEARANCE,
    val idleOpacityPct: Int = 40,
    val fadeDelayMs: Long = 4000L,
    val armingDelayMs: Long = 350L,
    val customTriggers: Boolean = false,
    // Hold-to-talk is the only default trigger; taps do nothing until the user opts in.
    val singleTapAction: WidgetTapAction = WidgetTapAction.None,
    val doubleTapAction: WidgetTapAction = WidgetTapAction.None,
    val soundMuted: Boolean = false,
    val motion: WidgetMotionSetting = WidgetMotionSetting.AUTO,
    val avoidKeyboard: Boolean = DataStoreKeys.DEFAULT_WIDGET_AVOID_KEYBOARD
) {
    companion object {
        fun fromPreferences(prefs: Preferences): WidgetConfig {
            val defaults = WidgetConfig()
            val custom = prefs[DataStoreKeys.WIDGET_CUSTOM_TRIGGERS] ?: defaults.customTriggers
            return WidgetConfig(
                enabled = prefs[DataStoreKeys.FLOATING_WIDGET_ENABLED]
                    ?: DataStoreKeys.DEFAULT_FLOATING_WIDGET_ENABLED,
                baseHeightDp = prefs[DataStoreKeys.WIDGET_BASE_HEIGHT_DP] ?: defaults.baseHeightDp,
                protrusionDp = prefs[DataStoreKeys.WIDGET_PROTRUSION_DP] ?: defaults.protrusionDp,
                edgeClearanceDp = prefs[DataStoreKeys.WIDGET_EDGE_CLEARANCE]
                    ?: defaults.edgeClearanceDp,
                idleOpacityPct = prefs[DataStoreKeys.WIDGET_IDLE_OPACITY_PCT] ?: defaults.idleOpacityPct,
                fadeDelayMs = prefs[DataStoreKeys.WIDGET_FADE_DELAY_MS] ?: defaults.fadeDelayMs,
                // The anti-accident delay applies always — it's a general safety slider,
                // not part of the custom tap overrides.
                armingDelayMs = prefs[DataStoreKeys.WIDGET_ARMING_DELAY_MS] ?: defaults.armingDelayMs,
                customTriggers = custom,
                singleTapAction = if (custom) {
                    WidgetTapAction.parse(prefs[DataStoreKeys.WIDGET_SINGLE_TAP_ACTION])
                } else {
                    defaults.singleTapAction
                },
                doubleTapAction = if (custom) {
                    WidgetTapAction.parse(prefs[DataStoreKeys.WIDGET_DOUBLE_TAP_ACTION])
                } else {
                    defaults.doubleTapAction
                },
                soundMuted = prefs[DataStoreKeys.WIDGET_SOUND_MUTED] ?: defaults.soundMuted,
                motion = WidgetMotionSetting.fromName(prefs[DataStoreKeys.WIDGET_REDUCED_MOTION]),
                avoidKeyboard = prefs[DataStoreKeys.WIDGET_AVOID_KEYBOARD]
                    ?: DataStoreKeys.DEFAULT_WIDGET_AVOID_KEYBOARD
            )
        }
    }
}
