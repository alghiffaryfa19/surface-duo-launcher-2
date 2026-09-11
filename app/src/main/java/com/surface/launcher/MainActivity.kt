package com.surface.launcher

import android.content.Context
import android.os.Bundle
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.microsoft.device.dualscreen.twopanelayout.TwoPaneLayout
import com.surface.launcher.ui.theme.DualScreenExperienceTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                                    apps = viewModel.pane1Apps,
                                    viewModel = viewModel,
                                    dragDropState = dragDropState
                                )
                            },
                            pane2 = {
                                PaneContent(
                                    paneId = 2,
                                    apps = viewModel.pane2Apps,
                                    viewModel = viewModel,
                                    dragDropState = dragDropState
                                )
                            }
                        )
                        
                        // Render dragged app
                        if (dragDropState.isDragging && dragDropState.draggedApp != null) {
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
                                AppIcon(app = dragDropState.draggedApp!!)
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
    apps: List<AppModel?>,
    viewModel: LauncherViewModel,
    dragDropState: DragDropState
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            val targetOffset = if (offsetX.value < -300f && paneId == 1) {
                                -1000f
                            } else if (offsetX.value > 300f && paneId == 1) {
                                1000f
                            } else {
                                0f
                            }
                            
                            offsetX.animateTo(targetOffset, animationSpec = tween(300))
                            
                            if (targetOffset == -1000f) {
                                viewModel.swipeLeft()
                                offsetX.snapTo(0f)
                            } else if (targetOffset == 1000f) {
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
        Box(modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(apps.size) { index ->
                    val app = apps[index]
                    AppSlot(
                        app = app,
                        paneId = paneId,
                        index = index,
                        viewModel = viewModel,
                        dragDropState = dragDropState
                    )
                }
            }
        }
    }
}

@Composable
fun AppSlot(
    app: AppModel?,
    paneId: Int,
    index: Int,
    viewModel: LauncherViewModel,
    dragDropState: DragDropState
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(
                if (app != null) Color.Transparent else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .pointerInput(app) {
                if (app != null) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            dragDropState.onDragStart(app, paneId, index, offset)
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
                if (app != null) {
                    try {
                        context.startActivity(app.intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            AppIcon(app = app)
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
