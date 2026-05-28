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
}