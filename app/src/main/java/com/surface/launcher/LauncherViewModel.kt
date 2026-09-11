package com.surface.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel

class LauncherViewModel : ViewModel() {
    
    val pane1Apps = mutableStateListOf<GridItem?>().apply {
        repeat(9) { add(null) }
    }
    
    val pane2Apps = mutableStateListOf<GridItem?>().apply {
        repeat(9) { add(null) }
    }
    
    val dockApps = mutableStateListOf<GridItem?>().apply {
        repeat(4) { add(null) }
    }
    
    private var allApps = listOf<AppModel>()
    private var pageIndex = 0

    fun loadInstalledApps(context: Context) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val activities = pm.queryIntentActivities(intent, 0)
        
        allApps = activities.mapIndexed { index, resolveInfo ->
            val iconDrawable = resolveInfo.loadIcon(pm)
            val bitmap = try {
                iconDrawable.toBitmap(width = 150, height = 150)
            } catch (e: Exception) {
                null
            }
            
            val componentName = android.content.ComponentName(
                resolveInfo.activityInfo.applicationInfo.packageName,
                resolveInfo.activityInfo.name
            )
            
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                component = componentName
            }
            
            AppModel(
                id = resolveInfo.activityInfo.packageName + index,
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                intent = launchIntent,
                iconBitmap = bitmap
            )
        }
        
        // Populate dock with first 4 apps if available
        for (i in 0 until minOf(4, allApps.size)) {
            dockApps[i] = allApps[i]
        }
        
        // Populate initial page (skip first 4 since they are in dock)
        pageIndex = 0
        populatePane2()
    }
    
    private fun populatePane2() {
        pane2Apps.clear()
        val startIndex = (pageIndex * 9) + 4 // offset for dock
        for (i in 0 until 9) {
            if (startIndex + i < allApps.size) {
                pane2Apps.add(allApps[startIndex + i])
            } else {
                pane2Apps.add(null)
            }
        }
    }

    fun moveItem(fromPane: Int, fromIndex: Int, toPane: Int, toIndex: Int) {
        val sourceList = when (fromPane) {
            1 -> pane1Apps
            2 -> pane2Apps
            else -> dockApps
        }
        val targetList = when (toPane) {
            1 -> pane1Apps
            2 -> pane2Apps
            else -> dockApps
        }

        val itemToMove = sourceList[fromIndex] ?: return
        
        val temp = targetList[toIndex]
        targetList[toIndex] = itemToMove
        sourceList[fromIndex] = temp
    }

    fun swipeLeft() {
        pane1Apps.clear()
        pane1Apps.addAll(pane2Apps)
        
        val maxPages = Math.ceil((allApps.size - 4) / 9.0).toInt()
        if (pageIndex < maxPages - 1) {
            pageIndex++
        }
        populatePane2()
    }
    
    fun swipeRight() {
        if (pageIndex > 0) {
            pane2Apps.clear()
            pane2Apps.addAll(pane1Apps)
            
            pageIndex--
            pane1Apps.clear()
            val previousStartIndex = ((pageIndex - 1) * 9) + 4
            for (i in 0 until 9) {
                if (previousStartIndex >= 4 && previousStartIndex + i < allApps.size) {
                    pane1Apps.add(allApps[previousStartIndex + i])
                } else {
                    pane1Apps.add(null)
                }
            }
        }
    }
}
