package com.surface.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.microsoft.device.dualscreen.twopanelayout.TwoPaneLayout
import com.surface.launcher.ui.theme.DualScreenExperienceTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, 1024)
        appWidgetHost.startListening()
        
        viewModel.loadInstalledApps(this)
        
        setContent {
            DualScreenExperienceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    val dragDropState = remember { DragDropState() }
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        TwoPaneLayout(
                            pane1 = {
                                PaneContent(
                                    paneId = 1,
                                    items = viewModel.pane1Apps,
                                    viewModel = viewModel,
                                    dragDropState = dragDropState,
                                    appWidgetHost = appWidgetHost,
                                    appWidgetManager = appWidgetManager
                                )
                            },
                            pane2 = {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PaneContent(
                                            paneId = 2,
                                            items = viewModel.pane2Apps,
                                            viewModel = viewModel,
                                            dragDropState = dragDropState,
                                            appWidgetHost = appWidgetHost,
                                            appWidgetManager = appWidgetManager
                                        )
                                    }
                                    
                                    // App Dock on the right
                                    Column(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .fillMaxHeight()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .padding(vertical = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        viewModel.dockApps.forEachIndexed { index, item ->
                                            Box(modifier = Modifier.size(64.dp)) {
                                                GridItemSlot(
                                                    item = item,
                                                    paneId = 3, // 3 denotes dock
                                                    index = index,
                                                    viewModel = viewModel,
                                                    dragDropState = dragDropState,
                                                    appWidgetHost = appWidgetHost,
                                                    appWidgetManager = appWidgetManager
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        
                        // Render dragged item
                        if (dragDropState.isDragging && dragDropState.draggedItem != null) {
                            Box(
                                modifier = Modifier
                                    .offset { 
                                        IntOffset(
                                            dragDropState.dragPosition.x.roundToInt() - 100, 
                                            dragDropState.dragPosition.y.roundToInt() - 100
                                        ) 
                                    }
                                    .size(80.dp)
                                    .zIndex(10f),
                                contentAlignment = Alignment.Center
                            ) {
                                when(val item = dragDropState.draggedItem) {
                                    is AppModel -> AppIcon(app = item)
                                    is WidgetModel -> Text("Widget", color = Color.White)
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaneContent(
    paneId: Int,
    items: List<GridItem?>,
    viewModel: LauncherViewModel,
    dragDropState: DragDropState,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    val maxDrag = 1000f
    val fraction = (Math.abs(offsetX.value) / maxDrag).coerceIn(0f, 1f)
    val scale = 1f - (fraction * 0.15f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            val targetOffset = if (offsetX.value < -250f && paneId == 1) {
                                -maxDrag
                            } else if (offsetX.value > 250f && paneId == 1) {
                                maxDrag
                            } else {
                                0f
                            }
                            
                            offsetX.animateTo(targetOffset, animationSpec = tween(300))
                            
                            if (targetOffset == -maxDrag) {
                                viewModel.swipeLeft()
                                offsetX.snapTo(0f)
                            } else if (targetOffset == maxDrag) {
                                viewModel.swipeRight()
                                offsetX.snapTo(0f)
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            if (paneId == 1) {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        }
                    }
                )
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - (fraction * 0.5f)
                }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    count = items.size,
                    span = { index -> 
                        val spanSize = items[index]?.spanX ?: 1
                        GridItemSpan(spanSize.coerceAtMost(3)) 
                    }
                ) { index ->
                    val item = items[index]
                    GridItemSlot(
                        item = item,
                        paneId = paneId,
                        index = index,
                        viewModel = viewModel,
                        dragDropState = dragDropState,
                        appWidgetHost = appWidgetHost,
                        appWidgetManager = appWidgetManager
                    )
                }
            }
        }
    }
}

@Composable
fun GridItemSlot(
    item: GridItem?,
    paneId: Int,
    index: Int,
    viewModel: LauncherViewModel,
    dragDropState: DragDropState,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .aspectRatio(if (item is WidgetModel) item.spanX.toFloat() else 1f)
            .background(
                if (item != null) Color.Transparent else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .pointerInput(item) {
                if (item != null) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            dragDropState.onDragStart(item, paneId, index, offset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDropState.onDrag(dragAmount)
                        },
                        onDragEnd = {
                            dragDropState.onDragInterrupt()
                        },
                        onDragCancel = {
                            dragDropState.onDragInterrupt()
                        }
                    )
                }
            }
            .clickable {
                if (item is AppModel) {
                    try {
                        context.startActivity(item.intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (item != null) {
            when (item) {
                is AppModel -> AppIcon(app = item)
                is WidgetModel -> WidgetView(item, appWidgetHost, appWidgetManager)
            }
        }
    }
}

@Composable
fun AppIcon(app: AppModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (app.iconBitmap != null) {
            Image(
                bitmap = app.iconBitmap.asImageBitmap(),
                contentDescription = app.name,
                modifier = Modifier.size(56.dp)
            )
        } else {
            Box(
                modifier = Modifier.size(56.dp).background(Color.Gray, RoundedCornerShape(8.dp))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WidgetView(
    widgetModel: WidgetModel,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager
) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize().background(Color.White, RoundedCornerShape(16.dp)),
        factory = { ctx ->
            try {
                val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetModel.appWidgetId)
                if (appWidgetInfo != null) {
                    val hostView = appWidgetHost.createView(ctx, widgetModel.appWidgetId, appWidgetInfo)
                    hostView.setAppWidget(widgetModel.appWidgetId, appWidgetInfo)
                    hostView
                } else {
                    FrameLayout(ctx).apply { setBackgroundColor(android.graphics.Color.RED) }
                }
            } catch (e: Exception) {
                FrameLayout(ctx).apply { setBackgroundColor(android.graphics.Color.GRAY) }
            }
        }
    )
}
