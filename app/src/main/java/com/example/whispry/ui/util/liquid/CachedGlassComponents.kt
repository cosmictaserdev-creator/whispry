// SPDX-License-Identifier: AGPL-3.0-or-later
package com.example.whispry.ui.util.liquid

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun rememberCachedBackdrop(): LayerBackdrop {
    val backdrop = rememberLayerBackdrop()
    val cachedBitmap = LocalCachedGlassBackdrop.current

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08080F))) {
        if (cachedBitmap != null) {
            Image(
                bitmap = cachedBitmap,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            )
        } else {
            // Placeholder while bitmap is generating
            Box(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop))
        }
    }
    return backdrop
}
