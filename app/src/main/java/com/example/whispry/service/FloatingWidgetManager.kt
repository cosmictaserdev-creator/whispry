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
import androidx.datastore.preferences.core.edit
import com.example.whispry.data.local.datasource.DataStoreKeys
import com.example.whispry.data.local.datasource.SettingsProvider
import com.example.whispry.ui.theme.WhispryTheme
import com.example.whispry.ui.theme.WhispryTokens
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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

    private val positionManager by lazy {
        BubblePositionManager(context.resources.displayMetrics.density)
    }

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        observeOverlayCoordinator()
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

    fun show() {
        if (composeView != null) return

        composeView = ComposeView(context).apply {
            setContent {
                val state by widgetState.collectAsState()
                WidgetUI(
                    state = state,
                    onToggleExpand = { widgetState.update { it.copy(isExpanded = !it.isExpanded) } },
                    onRecord = { serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStarted) },
                    onCopy = { 
                        state.lastTranscript?.let { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Whispry", text)
                            clipboard.setPrimaryClip(clip)
                        }
                    },
                    onStop = { serviceBridge.emit(ServiceBridge.TriggerEvent.RecordingStopped) }
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
        }

        managerScope.launch {
            val safeBounds = getSafeBounds().toBubbleBounds()
            val savedXPct = settingsProvider.dataStore.data.first()[DataStoreKeys.BUBBLE_POSITION_X]
            val savedYPct = settingsProvider.dataStore.data.first()[DataStoreKeys.BUBBLE_POSITION_Y]
            val initialSize = 48.dpToPx().toFloat()

            layoutParams?.let { lp ->
                if (savedXPct != null && savedYPct != null) {
                    val (x, y) = positionManager.denormalize(
                        savedXPct.toFloat(), savedYPct.toFloat(), safeBounds
                    )
                    lp.x = x.toInt()
                    lp.y = y.toInt()
                } else {
                    val (x, y) = positionManager.defaultPosition(
                        initialSize, initialSize, safeBounds
                    )
                    lp.x = x.toInt()
                    lp.y = y.toInt()
                }
            }

            try {
                windowManager.addView(composeView, layoutParams)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding widget", e)
            }
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
            label = "WidgetWidth",
            finishedListener = { updateWindowSize() }
        )
        val height by animateDpAsState(
            targetValue = if (state.isExpanded) 64.dp else 48.dp,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "WidgetHeight"
        )

        LaunchedEffect(state.isExpanded) {
            if (state.isExpanded) {
                reSnapAfterResize()
            } else {
                reSnapAfterResize()
            }
        }

        Box(
            modifier = Modifier
                .size(width, height)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
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
                        onDragEnd = { snapAndSavePosition() }
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
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

    private fun updateWindowSize() {
        layoutParams?.let { lp ->
            try {
                windowManager.updateViewLayout(composeView, lp)
            } catch (e: Exception) {}
        }
    }

    private fun snapAndSavePosition() {
        val view = composeView ?: return
        layoutParams?.let { lp ->
            val bounds = getSafeBounds().toBubbleBounds()
            val viewWidth = if (view.width > 0) view.width else 48.dpToPx()
            val viewHeight = if (view.height > 0) view.height else 48.dpToPx()

            val (snapX, snapY) = positionManager.snapPosition(
                lp.x.toFloat(), lp.y.toFloat(),
                viewWidth.toFloat(), viewHeight.toFloat(), bounds
            )
            lp.x = snapX.toInt()
            lp.y = snapY.toInt()
            try {
                windowManager.updateViewLayout(view, lp)
            } catch (e: Exception) {}

            val (nx, ny) = positionManager.normalize(
                lp.x.toFloat(), lp.y.toFloat(), bounds
            )
            managerScope.launch {
                settingsProvider.dataStore.edit { prefs ->
                    prefs[DataStoreKeys.BUBBLE_POSITION_X] = nx.toInt()
                    prefs[DataStoreKeys.BUBBLE_POSITION_Y] = ny.toInt()
                }
            }
        }
    }

    private fun reSnapAfterResize() {
        val view = composeView ?: return
        layoutParams?.let { lp ->
            val bounds = getSafeBounds().toBubbleBounds()
            val viewWidth = if (view.width > 0) view.width else 48.dpToPx()
            val viewHeight = if (view.height > 0) view.height else 48.dpToPx()
            val (snapX, snapY) = positionManager.snapPosition(
                lp.x.toFloat(), lp.y.toFloat(),
                viewWidth.toFloat(), viewHeight.toFloat(), bounds
            )
            lp.x = snapX.toInt()
            lp.y = snapY.toInt()
            try {
                windowManager.updateViewLayout(view, lp)
            } catch (e: Exception) {}
        }
    }

    private fun Int.dpToPx(): Int = (this * context.resources.displayMetrics.density).toInt()

    private fun android.graphics.Rect.toBubbleBounds() = BubbleBounds(left, top, right, bottom)

    private fun getSafeBounds(): android.graphics.Rect {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout()
            )
            android.graphics.Rect(
                bounds.left + insets.left,
                bounds.top + insets.top,
                bounds.right - insets.right,
                bounds.bottom - insets.bottom
            )
        } else {
            val dm = context.resources.displayMetrics
            android.graphics.Rect(0, 0, dm.widthPixels, dm.heightPixels)
        }
    }
}
