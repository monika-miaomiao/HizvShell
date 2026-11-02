package com.hizv.shell.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.hizv.shell.model.AppInfo

object AppRepository {

    /** 轻量加载应用列表（不加载图标），必须在后台线程调用 */
    fun getInstalledApps(context: Context, includeSystem: Boolean = true): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.map { info ->
            AppInfo(
                packageName = info.packageName,
                label = pm.getApplicationLabel(info).toString(),
                isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                isEnabled = info.enabled,
                versionName = try {
                    pm.getPackageInfo(info.packageName, 0).versionName ?: ""
                } catch (_: Exception) { "" },
                uid = info.uid
            )
        }.filter { includeSystem || !it.isSystemApp }
            .sortedBy { it.label.lowercase() }
    }

    /** 单独加载图标（按需调用，避免 ANR） */
    fun getIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) { null }
    }
}
