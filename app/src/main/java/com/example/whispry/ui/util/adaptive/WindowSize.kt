package com.example.whispry.ui.util.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration

@Immutable
enum class DeviceType { Phone, Tablet }

@Composable
fun currentDeviceType(): DeviceType {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp >= 600 -> DeviceType.Tablet
        else -> DeviceType.Phone
    }
}
