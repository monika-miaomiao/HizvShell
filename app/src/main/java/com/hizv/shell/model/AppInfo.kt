package com.hizv.shell.model

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val versionName: String,
    val uid: Int
)
