package com.example.whispry.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.whispry.domain.model.VoiceAppAction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes a [VoiceAppAction] by building and launching the appropriate Android intent.
 * Lives in the service layer because it needs a [Context] to resolve/launch apps.
 */
@Singleton
class VoiceCommandExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    sealed interface ExecResult {
        /** App/search launched successfully; [message] is the bubble status. */
        data class Launched(val message: String) : ExecResult
        /** Could not launch; the service should fall back to pasting the original transcript. */
        data class Failed(val message: String) : ExecResult
    }

    private companion object {
        const val CHROME_PKG = "com.android.chrome"
        const val YOUTUBE_PKG = "com.google.android.youtube"
        const val MAPS_PKG = "com.google.android.apps.maps"
    }

    fun execute(action: VoiceAppAction): ExecResult = when (action) {
        is VoiceAppAction.WebSearch -> webSearch(action.query)
        is VoiceAppAction.YoutubeSearch -> youtubeSearch(action.query)
        is VoiceAppAction.MapsSearch -> mapsSearch(action.query)
        is VoiceAppAction.PlayStoreSearch -> playStoreSearch(action.query)
        is VoiceAppAction.CreateNote -> createNote(action)
        is VoiceAppAction.OpenApp -> openApp(action)
    }

    private fun createNote(action: VoiceAppAction.CreateNote): ExecResult {
        // ACTION_SEND text/plain is the universal way to hand a new note to a notes app
        // (Keep, Samsung Notes, OneNote, etc. all accept it and create a pre-filled note).
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, action.text)
            putExtra(Intent.EXTRA_SUBJECT, "Note")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Pinned note app: send straight to it (if still installed).
        if (action.packageName.isNotBlank() && isInstalled(action.packageName)) {
            val direct = Intent(send).setPackage(action.packageName)
            if (launch(direct)) return ExecResult.Launched("Saving note to ${action.label}…")
        }

        // Otherwise let the user pick a note app once (Android remembers the default).
        val chooser = Intent.createChooser(send, "Save note to…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (launch(chooser)) ExecResult.Launched("Saving note…")
        else ExecResult.Failed("No notes app found")
    }

    private fun webSearch(query: String): ExecResult {
        val url = if (query.isBlank()) "https://www.google.com"
        else "https://www.google.com/search?q=${enc(query)}"
        val intent = viewIntent(url)
        // Prefer Chrome specifically when installed (per design), else default browser.
        if (isInstalled(CHROME_PKG)) intent.setPackage(CHROME_PKG)
        if (launch(intent)) return ExecResult.Launched("Searching the web…")
        // Chrome may have been disabled between the check and launch — retry with no package.
        return if (launch(viewIntent(url))) ExecResult.Launched("Searching the web…")
        else ExecResult.Failed("No browser found")
    }

    private fun youtubeSearch(query: String): ExecResult {
        val url = if (query.isBlank()) "https://www.youtube.com"
        else "https://www.youtube.com/results?search_query=${enc(query)}"
        val intent = viewIntent(url)
        if (isInstalled(YOUTUBE_PKG)) intent.setPackage(YOUTUBE_PKG)
        if (launch(intent)) return ExecResult.Launched("Opening YouTube…")
        return if (launch(viewIntent(url))) ExecResult.Launched("Opening YouTube…")
        else ExecResult.Failed("Couldn't open YouTube")
    }

    private fun mapsSearch(query: String): ExecResult {
        if (query.isNotBlank()) {
            val geo = viewIntent("geo:0,0?q=${enc(query)}")
            if (isInstalled(MAPS_PKG)) geo.setPackage(MAPS_PKG)
            if (launch(geo)) return ExecResult.Launched("Opening Maps…")
        }
        val web = viewIntent("https://www.google.com/maps/search/${enc(query)}")
        return if (launch(web)) ExecResult.Launched("Opening Maps…")
        else ExecResult.Failed("Couldn't open Maps")
    }

    private fun playStoreSearch(query: String): ExecResult {
        val market = viewIntent("market://search?q=${enc(query)}")
        if (launch(market)) return ExecResult.Launched("Opening Play Store…")
        val web = viewIntent("https://play.google.com/store/search?q=${enc(query)}")
        return if (launch(web)) ExecResult.Launched("Opening Play Store…")
        else ExecResult.Failed("Couldn't open Play Store")
    }

    private fun openApp(action: VoiceAppAction.OpenApp): ExecResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(action.packageName)
            ?: return ExecResult.Failed("${action.label} is not installed")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val hasPayload = action.clipboardPayload.isNotBlank()
        if (hasPayload) copyToClipboard(action.clipboardPayload)
        return if (launch(launchIntent)) {
            if (hasPayload) ExecResult.Launched("Opening ${action.label} — text copied ✓")
            else ExecResult.Launched("Opening ${action.label}…")
        } else {
            ExecResult.Failed("Couldn't open ${action.label}")
        }
    }

    private fun viewIntent(uri: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    private fun isInstalled(pkg: String): Boolean = try {
        context.packageManager.getLaunchIntentForPackage(pkg) != null
    } catch (e: Exception) {
        false
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("whispry_command", text))
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
