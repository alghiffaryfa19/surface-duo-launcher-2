package com.surface.launcher

import android.content.Intent
import android.graphics.Bitmap

data class AppModel(
    val id: String,
    val name: String,
    val packageName: String,
    val intent: Intent,
    val iconBitmap: Bitmap? = null
)
