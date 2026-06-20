package com.example.whispry.ui.util.liquid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.ui.theme.AccentPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlassBackdropCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider
) {
    private val _bitmap = MutableStateFlow<ImageBitmap?>(null)
    val bitmap: StateFlow<ImageBitmap?> = _bitmap.asStateFlow()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observationJob: Job? = null

    fun init() {
        if (observationJob?.isActive == true) return
        
        observationJob = scope.launch {
            settingsProvider.accentColor
                .distinctUntilChanged()
                .collect { _ ->
                    val newBitmap = buildBackgroundBitmap()
                    _bitmap.value = newBitmap
                }
        }
    }

    fun release() {
        observationJob?.cancel()
        observationJob = null
        _bitmap.value = null
    }

    private suspend fun buildBackgroundBitmap(): ImageBitmap {
        val dm = context.resources.displayMetrics
        // Performance optimization: Generate at 1/4 resolution
        // The GPU will scale this up instantly with bilinear filtering
        val factor = 4 
        val w = (dm.widthPixels / factor).coerceAtLeast(1)
        val h = (dm.heightPixels / factor).coerceAtLeast(1)

        val small = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(small)

        canvas.drawColor(0xFF08080F.toInt())

        val accentName = settingsProvider.accentColor.first()
        val preset = AccentPreset.entries.find { it.name == accentName } ?: AccentPreset.Purple
        val mainColor = preset.mainColor

        val glowPaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                w / 2f,
                h * 0.95f, 
                h * 0.75f, 
                intArrayOf(
                    Color.argb(90, (mainColor.red * 255).toInt(), (mainColor.green * 255).toInt(), (mainColor.blue * 255).toInt()),
                    Color.argb(40, (mainColor.red * 180).toInt(), (mainColor.green * 180).toInt(), (mainColor.blue * 180).toInt()),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glowPaint)

        // Light blur at low res is very effective after scaling
        val blurred = blurBitmap(small, radius = 4f)
        small.recycle()

        // NO upscaling here. Return the small bitmap.
        val finalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurred.copy(Bitmap.Config.HARDWARE, false).also { blurred.recycle() }
        } else {
            blurred
        }

        return finalBitmap.asImageBitmap()
    }

    private fun blurBitmap(bitmap: Bitmap, radius: Float): Bitmap {
        @Suppress("DEPRECATION")
        val rs = RenderScript.create(context)
        @Suppress("DEPRECATION")
        val input = Allocation.createFromBitmap(rs, bitmap)
        @Suppress("DEPRECATION")
        val output = Allocation.createTyped(rs, input.type)
        @Suppress("DEPRECATION")
        val blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        @Suppress("DEPRECATION")
        blurScript.setRadius(radius.coerceIn(1f, 25f))
        @Suppress("DEPRECATION")
        blurScript.setInput(input)
        @Suppress("DEPRECATION")
        blurScript.forEach(output)
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        @Suppress("DEPRECATION")
        output.copyTo(result)
        @Suppress("DEPRECATION")
        input.destroy()
        @Suppress("DEPRECATION")
        output.destroy()
        @Suppress("DEPRECATION")
        rs.destroy()
        return result
    }
}
