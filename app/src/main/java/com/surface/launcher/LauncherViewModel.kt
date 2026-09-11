package com.surface.launcher

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class LauncherViewModel : ViewModel() {
    // Each pane is just a list of AppModels.
    // For simplicity, pane1 is the left pane, pane2 is the right pane.
    
    // Default pane 1 is empty, pane 2 has 9 apps (3x3 grid)
    val pane1Apps = mutableStateListOf<AppModel?>().apply {
        repeat(9) { add(null) } // 9 slots
    }
    
    val pane2Apps = mutableStateListOf<AppModel?>().apply {
        for (i in 0 until 9) {
            add(AppModel(id = "app_$i", name = "App $i"))
        }
    }

    // Function to handle dropping an app to a new slot
    fun moveApp(fromPane: Int, fromIndex: Int, toPane: Int, toIndex: Int) {
        val sourceList = if (fromPane == 1) pane1Apps else pane2Apps
        val targetList = if (toPane == 1) pane1Apps else pane2Apps

        val appToMove = sourceList[fromIndex] ?: return
        
        // If target slot has an app, swap them. Otherwise, just move.
        val temp = targetList[toIndex]
        targetList[toIndex] = appToMove
        sourceList[fromIndex] = temp
    }

    // Swipe left logic: Move pane2 to pane1, load new apps into pane2
    private var pageIndex = 1
    fun swipeLeft() {
        // Move pane2 contents to pane1
        pane1Apps.clear()
        pane1Apps.addAll(pane2Apps)
        
        // Load new apps into pane2
        pane2Apps.clear()
        pageIndex++
        for (i in 0 until 9) {
            val appIndex = (pageIndex * 9) + i
            pane2Apps.add(AppModel(id = "app_$appIndex", name = "App $appIndex"))
        }
    }
    
    // Swipe right logic: Move pane1 to pane2, load previous apps into pane1
    fun swipeRight() {
        if (pageIndex > 0) {
            pane2Apps.clear()
            pane2Apps.addAll(pane1Apps)
            
            pane1Apps.clear()
            pageIndex--
            for (i in 0 until 9) {
                val appIndex = (pageIndex * 9) + i
                pane1Apps.add(AppModel(id = "app_$appIndex", name = "App $appIndex"))
            }
        }
    }
}
