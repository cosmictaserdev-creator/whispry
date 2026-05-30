package com.example.whispry.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object OemBatteryOptimization {
    
    fun shouldShowPrompt(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val knownAggressiveOems = listOf("xiaomi", "oppo", "vivo", "oneplus", 
                                          "huawei", "honor", "samsung")
        return knownAggressiveOems.any { manufacturer.contains(it) }
    }

    fun getSettingsIntent(context: Context): Intent? {
        return when (Build.MANUFACTURER.lowercase()) {
            "xiaomi"  -> Intent().setComponent(ComponentName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            )).apply {
                putExtra("package_name", context.packageName)
                putExtra("package_label", "Whispry")
            }
            "oppo"    -> Intent().setComponent(ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.FakeActivity"
            ))
            "samsung" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            "huawei" -> Intent().setComponent(ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            ))
            else -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
    }
}
