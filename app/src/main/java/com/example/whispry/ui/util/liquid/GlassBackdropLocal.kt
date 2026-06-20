package com.example.whispry.ui.util.liquid

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle

val LocalCachedGlassBackdrop = staticCompositionLocalOf<ImageBitmap?> { null }

@Composable
fun CachedGlassProvider(
    cache: GlassBackdropCache,
    content: @Composable () -> Unit
) {
    val bitmap by cache.bitmap.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalCachedGlassBackdrop provides bitmap,
        content = content
    )
}
