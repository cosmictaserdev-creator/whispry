// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater.presentation

import com.example.whispry.updater.UpdateRelease
import java.io.File

data class UpdateScreenState(
    val currentVersion: String,
    val phase: UpdatePhase = UpdatePhase.Idle,
    val downloadProgressPct: Int = 0,
    val needsInstallPermission: Boolean = false
)

sealed interface UpdatePhase {
    data object Idle : UpdatePhase
    data object Checking : UpdatePhase
    data object UpToDate : UpdatePhase
    data class Available(val release: UpdateRelease) : UpdatePhase
    data class Downloading(val release: UpdateRelease) : UpdatePhase
    data class ReadyToInstall(val release: UpdateRelease, val apkFile: File) : UpdatePhase
    data class Error(val message: String) : UpdatePhase
}

sealed interface UpdateScreenIntent {
    data object CheckForUpdate : UpdateScreenIntent
    data object DownloadAndInstall : UpdateScreenIntent
    data object OpenInstallPermissionSettings : UpdateScreenIntent
    data object InstallPermissionDialogDismissed : UpdateScreenIntent
}
