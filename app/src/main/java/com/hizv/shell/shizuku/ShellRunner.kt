package com.hizv.shell.shizuku

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

/**
 * 通过 Shizuku 执行 shell 命令。
 * 关键：传入完整的 ADB shell 环境变量，否则 pm/am/settings 等命令找不到。
 */
object ShellRunner {

    private val newProcessMethod: Method by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    /** 模拟 adb shell 的环境变量 */
    private val shellEnv: Array<String> = arrayOf(
        "PATH=/system/bin:/system/xbin:/vendor/bin:/vendor/xbin:/sbin:/odm/bin:/product/bin",
        "ANDROID_ROOT=/system",
        "ANDROID_DATA=/data",
        "ANDROID_TZ=UTC",
        "EXTERNAL_STORAGE=/sdcard",
        "HOME=/sdcard",
        "TMPDIR=/data/local/tmp",
        "BOOTCLASSPATH=/system/framework/core.jar:/system/framework/core-oj.jar:/system/framework/conscrypt.jar:/system/framework/bouncycastle.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/framework-graphics.jar:/system/framework/telephony-common.jar:/system/framework/voip-common.jar:/system/framework/ims-common.jar:/system/framework/apache-xml.jar:/system/framework/okhttp.jar:/system/framework/okhttp3.jar:/system/framework/org.apache.http.legacy.boot.jar:/system/framework/services.jar",
        "LD_LIBRARY_PATH=/system/lib:/system/lib64:/vendor/lib:/vendor/lib64"
    )

    data class Result(
        val exitCode: Int,
        val output: String
    )

    private fun spawnProcess(cmd: Array<String>): Process {
        // 关键：传入环境变量数组，dir 设为 /sdcard
        return newProcessMethod.invoke(null, cmd, shellEnv, "/sdcard") as Process
    }

    /**
     * 执行单条命令并阻塞等待结果。必须在 IO 线程调用。
     */
    fun exec(command: String): Result {
        return try {
            // 用 login shell 确保 profile 被加载，同时显式 export PATH
            val wrappedCmd = "export PATH=/system/bin:/system/xbin:/vendor/bin:/sbin; $command"
            val process = spawnProcess(arrayOf("sh", "-c", wrappedCmd))
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val code = process.waitFor()
            Result(code, buildString {
                if (stdout.isNotBlank()) append(stdout)
                if (stderr.isNotBlank()) {
                    if (isNotEmpty()) append('\n')
                    append(stderr)
                }
            }.trim())
        } catch (e: Exception) {
            Result(-1, "执行失败: ${e.message}")
        }
    }

    /**
     * 执行命令并实时回调输出行。
     */
    fun execStreaming(command: String, onLine: (String) -> Unit, onDone: (Int) -> Unit) {
        try {
            val wrappedCmd = "export PATH=/system/bin:/system/xbin:/vendor/bin:/sbin; $command"
            val process = spawnProcess(arrayOf("sh", "-c", wrappedCmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            Thread {
                reader.forEachLine { onLine(it) }
                errReader.forEachLine { onLine("[stderr] $it") }
                val code = process.waitFor()
                onDone(code)
            }.start()
        } catch (e: Exception) {
            onLine("执行失败: ${e.message}")
            onDone(-1)
        }
    }
}
