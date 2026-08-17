// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            startServices(context)
        }
    }

    private fun startServices(context: Context) {
        val bubbleIntent = Intent(context, BubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(bubbleIntent)
        } else {
            context.startService(bubbleIntent)
        }
        
        // TriggerService is an AccessibilityService, it's managed by the system
        // once enabled by the user. We don't need to manually start it.
    }
}
