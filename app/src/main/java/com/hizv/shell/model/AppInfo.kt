package com.hizv.shell.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val versionName: String,
    val uid: Int
)
