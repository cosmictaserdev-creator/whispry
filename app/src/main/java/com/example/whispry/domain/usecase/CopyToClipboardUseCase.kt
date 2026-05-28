package com.example.whispry.domain.usecase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CopyToClipboardUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Whispry Transcription", text)
        clipboard.setPrimaryClip(clip)
    }
}
