// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater.presentation

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whispry.BuildConfig
import com.example.whispry.updater.DownloadState
import com.example.whispry.updater.UpdateCheckResult
import com.example.whispry.updater.UpdateDownloader
import com.example.whispry.updater.UpdateInstaller
import com.example.whispry.updater.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository,
    private val downloader: UpdateDownloader,
    private val installer: UpdateInstaller,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateScreenState(currentVersion = BuildConfig.VERSION_NAME))
    val state: StateFlow<UpdateScreenState> = _state.asStateFlow()

    init {
        onIntent(UpdateScreenIntent.CheckForUpdate)
    }

    fun onIntent(intent: UpdateScreenIntent) {
        when (intent) {
            UpdateScreenIntent.CheckForUpdate -> checkForUpdate()
            UpdateScreenIntent.DownloadAndInstall -> downloadAndInstall()
            UpdateScreenIntent.OpenInstallPermissionSettings -> {
                _state.update { it.copy(needsInstallPermission = false) }
                context.startActivity(
                    installer.requestInstallPermissionIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            UpdateScreenIntent.InstallPermissionDialogDismissed ->
                _state.update { it.copy(needsInstallPermission = false) }
        }
    }

    private fun checkForUpdate() {
        _state.update { it.copy(phase = UpdatePhase.Checking) }
        viewModelScope.launch {
            repository.checkForUpdate()
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            phase = when (result) {
                                UpdateCheckResult.UpToDate -> UpdatePhase.UpToDate
                                is UpdateCheckResult.Available -> UpdatePhase.Available(result.release)
                            }
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(phase = UpdatePhase.Error(e.message ?: "Couldn't check for updates")) }
                }
        }
    }

    private fun downloadAndInstall() {
        // Re-tapping "Download & Install" after granting the install-unknown-apps permission is
        // the retry path — there's no lifecycle observer re-checking permission automatically.
        val release = (_state.value.phase as? UpdatePhase.Available)?.release ?: return
        if (!installer.canInstallPackages()) {
            _state.update { it.copy(needsInstallPermission = true) }
            return
        }
        viewModelScope.launch {
            downloader.download(release.downloadUrl, release.assetName).collect { downloadState ->
                when (downloadState) {
                    is DownloadState.InProgress -> _state.update {
                        it.copy(phase = UpdatePhase.Downloading(release), downloadProgressPct = downloadState.progressPct)
                    }
                    is DownloadState.Done -> {
                        _state.update { it.copy(phase = UpdatePhase.ReadyToInstall(release, downloadState.file)) }
                        installer.install(downloadState.file)
                    }
                    is DownloadState.Failed -> _state.update {
                        it.copy(phase = UpdatePhase.Error(downloadState.reason))
                    }
                }
            }
        }
    }
}
