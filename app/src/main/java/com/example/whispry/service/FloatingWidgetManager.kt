package com.example.whispry.service

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class FloatingWidgetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider,
    private val serviceBridge: ServiceBridge,
    private val overlayCoordinator: WindowOverlayCoordinator
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val TAG = "Whispry_WidgetManager"

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val widgetState = MutableStateFlow(FloatingWidgetState())

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        observeOverlayCoordinator()
        observeTranscriptionResults()
    }

    private fun observeOverlayCoordinator() {
        managerScope.launch {
            overlayCoordinator.visibleOverlay.collect { type ->
                if (type == WindowOverlayCoordinator.OverlayType.Widget) {
                    show()
                } else {
                    hide()
                }
            }
        }
    }

    private fun observeTranscriptionResults() {
        managerScope.launch {
            serviceBridge.triggerEvent.collect { event ->
                if (event is ServiceBridge.TriggerEvent.TranscriptionResult) {
                    widgetState.update { it.copy(lastTranscript = event.text) }
                }
            }
        }
    }

    fun show() {
        if (composeView != null) return

        composeView = ComposeView(context).apply {
            setContent {
                val state by widgetState.collectAsState()
                WidgetUI(
                    state = state,
                    onToggleExpand = { widgetState.update { it.copy(isExpanded = !it.isExpanded) } },
                    onRecord = { serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted) },
                    onCopy = { /* Copy logic */ },
                    onStop = { /* Stop logic */ }
                )
            }
            setViewTreeLifecycleOwner(this@FloatingWidgetManager)
            setViewTreeViewModelStoreOwner(this@FloatingWidgetManager)
            setViewTreeSavedStateRegistryOwner(this@FloatingWidgetManager)
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16.dpToPx()
            y = 120.dpToPx()
        }

        try {
            windowManager.addView(composeView, layoutParams)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding widget", e)
        }
    }

    fun hide() {
        composeView?.let {
            try {
                windowManager.removeView(it)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            } catch (e: Exception) {}
        }
        composeView = null
        layoutParams = null
    }

    @Composable
    private fun WidgetUI(
        state: FloatingWidgetState,
        onToggleExpand: () -> Unit,
        onRecord: () -> Unit,
        onCopy: () -> Unit,
        onStop: () -> Unit
    ) {
        val width by animateDpAsState(
            targetValue = if (state.isExpanded) 220.dp else 48.dp,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "WidgetWidth"
        )
        val height by animateDpAsState(
            targetValue = if (state.isExpanded) 140.dp else 48.dp,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "WidgetHeight"
        )

        val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Breathing"
        )

        Box(
            modifier = Modifier
                .size(width, height)
                .scale(if (state.isExpanded) 1f else scale)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleExpand() }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            layoutParams?.let { lp ->
                                lp.x += dragAmount.x.roundToInt()
                                lp.y += dragAmount.y.roundToInt()
                                try {
                                    windowManager.updateViewLayout(composeView, lp)
                                } catch (e: Exception) {}
                            }
                        },
                        onDragEnd = {
                            snapToEdge()
                        }
                    )
                }
        ) {
            if (!state.isExpanded) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Whispry",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Collapse",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onToggleExpand() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Transcript
                    Text(
                        text = state.lastTranscript ?: "Ready to listen",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = if (state.lastTranscript != null) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.4f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionChip(icon = Icons.Rounded.Mic, label = "Record", onClick = onRecord)
                        ActionChip(icon = Icons.Rounded.ContentCopy, label = "Copy", onClick = onCopy)
                        ActionChip(icon = Icons.Rounded.Stop, label = "Stop", onClick = onStop)
                    }
                }
            }
        }
    }

    @Composable
    private fun ActionChip(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        onClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable { onClick() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    private fun snapToEdge() {
        layoutParams?.let { lp ->
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthWidth
            val screenHeight = displayMetrics.heightHeight
            
            // Snap to nearest corner
            val targetX = if (lp.x < screenWidth / 2) 16.dpToPx() else screenWidth - lp.width - 16.dpToPx()
            val targetY = if (lp.y < screenHeight / 2) 16.dpToPx() else screenHeight - lp.height - 16.dpToPx()
            
            // Simple snap for now, can use animator for spring feel
            lp.x = targetX
            lp.y = targetY
            try {
                windowManager.updateViewLayout(composeView, lp)
            } catch (e: Exception) {}
        }
    }

    private fun Int.dpToPx(): Int = (this * context.resources.displayMetrics.density).toInt()
    
    private val android.util.DisplayMetrics.widthWidth: Int get() = widthPixels
    private val android.util.DisplayMetrics.heightHeight: Int get() = heightPixels
}
