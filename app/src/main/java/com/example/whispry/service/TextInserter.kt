package com.example.whispry.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextInserter @Inject constructor() {

    /**
     * Main entry point.
     * Always copies to clipboard.
     * Attempts to paste into the focused field via AccessibilityService.
     * Returns true if auto-paste succeeded, false if user needs to paste manually.
     */
    fun insertText(
        text: String,
        context: Context,
        accessibilityService: android.accessibilityservice.AccessibilityService?
    ): InsertResult {

        // Step 1 — always copy to clipboard first
        // this is the guaranteed fallback
        copyToClipboard(context, text)

        // Step 2 — attempt auto paste if we have a live service
        if (accessibilityService == null) {
            return InsertResult.COPIED_ONLY
        }

        val didPaste = attemptAutoPaste(accessibilityService)

        return if (didPaste) InsertResult.PASTED else InsertResult.COPIED_ONLY
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("whispry_transcript", text))
    }

    private fun attemptAutoPaste(
        service: android.accessibilityservice.AccessibilityService
    ): Boolean {

        // get the root node of the currently active window
        val rootNode = service.rootInActiveWindow ?: return false

        return try {
            // find the node that currently has keyboard/input focus
            val focusedNode = rootNode.findFocus(
                AccessibilityNodeInfo.FOCUS_INPUT
            ) ?: return false

            // check this node actually accepts text input
            // attempting paste on a non-editable node does nothing
            if (!focusedNode.isEditable) {
                focusedNode.recycle()
                return false
            }

            // perform the paste action
            val success = focusedNode.performAction(
                AccessibilityNodeInfo.ACTION_PASTE
            )

            focusedNode.recycle()
            success

        } catch (e: Exception) {
            // some apps throw when you try to access their nodes
            // (banking apps, secure fields) — fail gracefully
            false
        } finally {
            rootNode.recycle()
        }
    }

    enum class InsertResult {
        PASTED,       // auto-paste succeeded — fully seamless
        COPIED_ONLY   // fallback — user needs to paste manually
    }
}