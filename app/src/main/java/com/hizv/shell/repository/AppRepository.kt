package com.hizv.shell.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.hizv.shell.model.AppInfo

object AppRepository {

    fun getInstalledApps(context: Context, includeSystem: Boolean = true): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.map { info ->
            AppInfo(
                packageName = info.packageName,
                label = pm.getApplicationLabel(info).toString(),
                icon = pm.getApplicationIcon(info),
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
}
