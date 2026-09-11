package com.surface.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class DragDropState {
    var isDragging by mutableStateOf(false)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    
    var draggedApp by mutableStateOf<AppModel?>(null)
    var sourcePane by mutableStateOf(-1)
    var sourceIndex by mutableStateOf(-1)

    fun onDragStart(app: AppModel, pane: Int, index: Int, offset: Offset) {
        isDragging = true
        draggedApp = app
        sourcePane = pane
        sourceIndex = index
        dragPosition = offset
    }

    fun onDrag(offset: Offset) {
        dragPosition += offset
    }

    fun onDragInterrupt() {
        isDragging = false
        draggedApp = null
        sourcePane = -1
        sourceIndex = -1
        dragOffset = Offset.Zero
    }
}
