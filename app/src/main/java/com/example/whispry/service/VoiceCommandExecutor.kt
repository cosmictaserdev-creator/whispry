// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
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
        val CALCULATOR_PKGS = setOf(
            "com.google.android.calculator",
            "com.sec.android.app.popupcalculator",
            "com.samsung.android.calculator"
        )
    }

    fun execute(action: VoiceAppAction): ExecResult = when (action) {
        is VoiceAppAction.WebSearch -> webSearch(action.query)
        is VoiceAppAction.YoutubeSearch -> youtubeSearch(action.query)
        is VoiceAppAction.MapsSearch -> mapsSearch(action.query)
        is VoiceAppAction.PlayStoreSearch -> playStoreSearch(action.query)
        is VoiceAppAction.CreateNote -> createNote(action)
        is VoiceAppAction.OpenApp -> openApp(action)
        is VoiceAppAction.Calculate -> calculate(action)
        is VoiceAppAction.Call -> call(action)
        is VoiceAppAction.Sms -> sms(action)
        is VoiceAppAction.SetAlarm -> setAlarm(action)
        is VoiceAppAction.SetTimer -> setTimer(action)
        is VoiceAppAction.CalendarEvent -> calendarEvent(action)
        is VoiceAppAction.Email -> email(action)
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

    private fun calculate(action: VoiceAppAction.Calculate): ExecResult {
        val tokens = tokenizeExpression(action.expression)
        if (tokens.isEmpty()) return ExecResult.Failed("Couldn't understand that calculation")
        copyToClipboard(action.expression)
        val launchIntent = CALCULATOR_PKGS.firstNotNullOfOrNull { pkg ->
            context.packageManager.getLaunchIntentForPackage(pkg)
        } ?: return ExecResult.Failed("No calculator app found — copied to clipboard")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!launch(launchIntent)) return ExecResult.Failed("Couldn't open the calculator")
        ServiceLocator.triggerService?.performCalculatorInput(tokens, CALCULATOR_PKGS)
        return ExecResult.Launched("Calculating…")
    }

    /** Hand-rolled tokenizer — digits, one of +-×÷., ending with "=". No eval(), this only ever
     *  drives calculator button taps, never runs arithmetic itself. */
    private fun tokenizeExpression(raw: String): List<String> {
        val normalized = raw.lowercase()
            .replace(Regex("\\bplus\\b"), "+")
            .replace(Regex("\\b(minus|subtract)\\b"), "-")
            .replace(Regex("\\b(multiplied by|multiply|times)\\b"), "×")
            .replace(Regex("\\b(divided by|divide)\\b"), "÷")
            .replace(Regex("\\b(point|decimal)\\b"), ".")
            .replace(Regex("(?<=[\\d\\s])x(?=[\\d\\s])"), "×")
            .replace("*", "×")
            .replace("/", "÷")

        val tokens = mutableListOf<String>()
        for (c in normalized) {
            when {
                c.isDigit() -> tokens += c.toString()
                c == '+' || c == '-' || c == '×' || c == '÷' || c == '.' -> tokens += c.toString()
            }
        }
        if (tokens.isEmpty()) return emptyList()
        tokens += "="
        return tokens
    }

    private fun call(action: VoiceAppAction.Call): ExecResult {
        val digits = action.query.filter { it.isDigit() || it == '+' }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            if (digits.isNotBlank()) data = Uri.parse("tel:$digits")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (launch(intent)) ExecResult.Launched("Opening dialer…") else ExecResult.Failed("Couldn't open the dialer")
    }

    private fun sms(action: VoiceAppAction.Sms): ExecResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
            putExtra("sms_body", action.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (launch(intent)) ExecResult.Launched("Opening messages…") else ExecResult.Failed("Couldn't open messages")
    }

    private fun setAlarm(action: VoiceAppAction.SetAlarm): ExecResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            if (action.message.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, action.message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (launch(intent)) ExecResult.Launched("Opening alarm…") else ExecResult.Failed("Couldn't open the alarm app")
    }

    private fun setTimer(action: VoiceAppAction.SetTimer): ExecResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            if (action.durationSeconds != null) putExtra(AlarmClock.EXTRA_LENGTH, action.durationSeconds)
            if (action.message.isNotBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, action.message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (launch(intent)) ExecResult.Launched("Setting timer…") else ExecResult.Failed("Couldn't open the timer")
    }

    private fun calendarEvent(action: VoiceAppAction.CalendarEvent): ExecResult {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, action.title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (launch(intent)) ExecResult.Launched("Opening calendar…") else ExecResult.Failed("Couldn't open the calendar")
    }

    private fun email(action: VoiceAppAction.Email): ExecResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_TEXT, action.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (launch(intent)) ExecResult.Launched("Opening email…") else ExecResult.Failed("Couldn't open an email app")
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
