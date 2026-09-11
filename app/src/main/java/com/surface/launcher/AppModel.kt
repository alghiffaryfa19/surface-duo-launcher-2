package com.surface.launcher

data class AppModel(
    val id: String,
    val name: String,
    val iconRes: Int? = null,
    val colorHex: Long = 0xFF444444 // placeholder color
)
