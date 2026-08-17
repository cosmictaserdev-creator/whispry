// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object OemBatteryOptimization {

    private const val TAG = "OemBatteryOptimization"

    fun shouldShowPrompt(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val knownAggressiveOems = listOf("xiaomi", "poco", "oppo", "realme", "vivo", "oneplus",
                                          "huawei", "honor", "samsung")
        return knownAggressiveOems.any { manufacturer.contains(it) }
    }

    /** True once the OS itself won't doze/kill this app for battery reasons. OEM autostart/
     *  protected-app lists (MIUI, ColorOS, ...) sit on top of this and aren't queryable, so this
     *  is the best available signal for whether the user completed the prompt. */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Opens the OEM-specific battery/autostart settings. Some ROMs rename or drop the
     *  hardcoded component (region/version drift), which would otherwise crash with
     *  ActivityNotFoundException - fall back to the generic Android battery-optimization
     *  prompt when that happens. */
    fun openSettings(context: Context) {
        try {
            context.startActivity(getSettingsIntent(context))
        } catch (e: Exception) {
            Log.w(TAG, "OEM battery settings intent failed, falling back to generic prompt", e)
            try {
                context.startActivity(genericIgnoreBatteryOptimizationsIntent(context))
            } catch (e2: Exception) {
                Log.e(TAG, "No battery optimization settings available on this device", e2)
            }
        }
    }

    private fun genericIgnoreBatteryOptimizationsIntent(context: Context) =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }

    private fun getSettingsIntent(context: Context): Intent {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            // POCO ships HyperOS/MIUI -> the Xiaomi power-keeper flow.
            manufacturer.contains("poco") || manufacturer.contains("xiaomi") -> Intent().setComponent(ComponentName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            )).apply {
                putExtra("package_name", context.packageName)
                putExtra("package_label", "Whispry")
            }
            // realme runs ColorOS -> the OPPO safe-center flow.
            manufacturer.contains("realme") || manufacturer.contains("oppo") -> Intent().setComponent(ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.FakeActivity"
            ))
            manufacturer.contains("samsung") -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            ))
            else -> genericIgnoreBatteryOptimizationsIntent(context)
        }
    }
}
