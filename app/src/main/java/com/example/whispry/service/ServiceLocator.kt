// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import java.lang.ref.WeakReference

/**
 * Lightweight registry for service references.
 * Uses WeakReference so we never prevent garbage collection.
 */
object ServiceLocator {

    private var _triggerService: WeakReference<TriggerService>? = null

    var triggerService: TriggerService?
        get() = _triggerService?.get()
        set(value) {
            _triggerService = if (value != null) WeakReference(value) else null
        }

    @Volatile
    var lastForegroundPackage: String? = null

    /**
     * The app currently in the foreground, resolved on demand from the accessibility service's
     * window list (skips the IME window, which is a foreground window of its own). Falls back to
     * the last window-state-changed cache when the service isn't running. Shared by app-aware
     * tones and hidden-apps suppression.
     */
    fun currentForegroundApp(): String? {
        val service = triggerService ?: return lastForegroundPackage
        val appWindows = service.windows
            .filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }
        val appWindow = appWindows.firstOrNull { it.isActive } ?: appWindows.firstOrNull()
        // API 37 removed AccessibilityWindowInfo.getPackageName(); the root node still exposes it.
        return appWindow?.root?.packageName?.toString() ?: lastForegroundPackage
    }
}