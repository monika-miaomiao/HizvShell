package com.hizv.shell.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.hizv.shell.shizuku.ShellRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppDownloader {

    data class DownloadState(
        var progress: Int = 0,
        var downloaded: Long = 0,
        var total: Long = 0,
        var status: Status = Status.IDLE
    ) {
        enum class Status { IDLE, DOWNLOADING, DONE, ERROR, INSTALLING }
    }

    /**
     * 下载 APK 到缓存目录并通过 shell 静默安装。
     */
    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onProgress: (Int, Long, Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.connect()

            val total = conn.contentLengthLong
            val tmpFile = File(context.cacheDir, "update.apk")
            tmpFile.delete()

            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        val pct = if (total > 0) (downloaded * 100 / total).toInt() else 0
                        onProgress(pct, downloaded, total)
                    }
                }
            }
            conn.disconnect()

            // 通过 Shizuku shell 静默安装
            onProgress(100, total, total)
            val result = ShellRunner.exec("pm install -r -g ${tmpFile.absolutePath}")
            tmpFile.delete()

            result.exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 下载 APK 到 Download 目录并触发安装器（无需 Shizuku）。
     */
    suspend fun downloadAndOpenInstaller(
        context: Context,
        url: String,
        onProgress: (Int, Long, Long) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.connect()

            val total = conn.contentLengthLong
            val tmpFile = File(context.cacheDir, "update.apk")
            tmpFile.delete()

            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        val pct = if (total > 0) (downloaded * 100 / total).toInt() else 0
                        onProgress(pct, downloaded, total)
                    }
                }
            }
            conn.disconnect()

            // 触发系统安装器
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
            } else {
                Uri.fromFile(tmpFile)
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
