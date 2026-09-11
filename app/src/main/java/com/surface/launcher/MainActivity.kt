package com.surface.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.microsoft.device.dualscreen.twopanelayout.TwoPaneLayout
import com.surface.launcher.ui.theme.DualScreenExperienceTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                    .background(Color(dragDropState.draggedApp!!.colorHex), RoundedCornerShape(16.dp))
                                    .zIndex(10f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(dragDropState.draggedApp!!.name, color = Color.White)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { /* Snap logic if needed */ },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount < -50f && paneId == 1) {
                            viewModel.swipeLeft()
                        } else if (dragAmount > 50f && paneId == 1) {
                            viewModel.swipeRight()
                        }
                    }
                )
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
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

@Composable
fun AppSlot(
    app: AppModel?,
    paneId: Int,
    index: Int,
    viewModel: LauncherViewModel,
    dragDropState: DragDropState
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(
                if (app != null) Color(app.colorHex) else Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .pointerInput(app) {
                if (app != null) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragDropState.onDragStart(app, paneId, index, offset)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDropState.onDrag(dragAmount)
                        },
                        onDragEnd = {
                            // Check drop target logic here - simplified version
                            // In a real app we'd calculate intersection bounds.
                            dragDropState.onDragInterrupt()
                        },
                        onDragCancel = {
                            dragDropState.onDragInterrupt()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            Text(app.name, color = Color.White)
        }
    }
}
