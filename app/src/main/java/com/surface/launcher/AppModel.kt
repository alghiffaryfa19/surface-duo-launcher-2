package com.surface.launcher

import android.content.Intent
import android.graphics.Bitmap

data class AppModel(
    override val id: String,
    val name: String,
    val packageName: String,
    val intent: Intent,
    val iconBitmap: Bitmap? = null,
    override val spanX: Int = 1,
    override val spanY: Int = 1
) : GridItem
