package com.surface.launcher

data class WidgetModel(
    override val id: String,
    val appWidgetId: Int,
    override val spanX: Int = 2,
    override val spanY: Int = 1
) : GridItem
