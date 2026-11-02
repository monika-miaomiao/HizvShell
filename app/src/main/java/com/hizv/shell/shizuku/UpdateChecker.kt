package com.hizv.shell.shizuku

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Release 更新检查。
 * 检查 monika-miaomiao/HizvShell 仓库的最新 release。
 */
object UpdateChecker {

    data class UpdateInfo(
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val isUpdateAvailable: Boolean
    )

    const val CURRENT_VERSION = "1.2.1"
    private const val REPO_OWNER = "monika-miaomiao"
    private const val REPO_NAME = "HizvShell"
    private const val API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    var updateInfo by mutableStateOf<UpdateInfo?>(null)
    var checking by mutableStateOf(false)

    suspend fun check(): UpdateInfo? {
        checking = true
        return try {
            val conn = URL(API_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "v0.0.0")
            val latestVer = tagName.removePrefix("v")
            val notes = json.optString("body", "")
            val assets = json.optJSONArray("assets")
            var downloadUrl = ""
            if (assets != null && assets.length() > 0) {
                downloadUrl = assets.getJSONObject(0).optString("browser_download_url", "")
            }
            // 比较版本号
            val hasUpdate = compareVersion(latestVer, CURRENT_VERSION) > 0
            UpdateInfo(latestVer, notes, downloadUrl, hasUpdate).also {
                updateInfo = it
            }
        } catch (e: Exception) {
            null
        } finally {
            checking = false
        }
    }

    private fun compareVersion(a: String, b: String): Int {
        val partsA = a.split(".").mapNotNull { it.toIntOrNull() }
        val partsB = b.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val va = partsA.getOrElse(i) { 0 }
            val vb = partsB.getOrElse(i) { 0 }
            if (va != vb) return va - vb
        }
        return 0
    }

    fun openDownload(context: Context, url: String) {
        if (url.isNotBlank()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}
