// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** False if the OS will block a package-install intent from this app until the user grants
     *  "install unknown apps" for it. */
    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Sends the user to the system "install unknown apps" toggle for this app. */
    fun requestInstallPermissionIntent(): Intent =
        Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )

    /**
     * Hands the downloaded APK to the system installer. Uses ACTION_VIEW with a FileProvider URI
     * rather than the older ACTION_INSTALL_PACKAGE — the latter was effectively deprecated after
     * API 24 in favor of this, which is what the system installer UI expects on modern Android.
     */
    fun install(apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
