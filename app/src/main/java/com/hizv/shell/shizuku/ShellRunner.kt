package com.hizv.shell.shizuku

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

/**
 * 通过 Shizuku 执行 shell 命令。
 * 关键：设置正确的 apex linker 环境变量，不能覆盖 LD_LIBRARY_PATH。
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

    data class Result(
        val exitCode: Int,
        val output: String
    )

    /** 最小化环境：只设 apex root + PATH，不碰 LD_LIBRARY_PATH */
    private val shellEnv: Array<String> = arrayOf(
        "ANDROID_RUNTIME_ROOT=/apex/com.android.runtime",
        "ANDROID_TZDATA_ROOT=/apex/com.android.tzdata",
        "ANDROID_I18N_ROOT=/apex/com.android.i18n",
        "PATH=/system/bin:/system/xbin:/vendor/bin:/vendor/xbin:/odm/bin:/product/bin:/sbin",
        "ANDROID_ROOT=/system",
        "ANDROID_DATA=/data",
        "EXTERNAL_STORAGE=/sdcard",
        "HOME=/sdcard",
        "TMPDIR=/data/local/tmp"
    )

    private fun spawnProcess(cmd: Array<String>): Process {
        return newProcessMethod.invoke(null, cmd, shellEnv, "/sdcard") as Process
    }

    fun exec(command: String): Result {
        return try {
            val process = spawnProcess(arrayOf("sh", "-c", command))
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

    fun execStreaming(command: String, onLine: (String) -> Unit, onDone: (Int) -> Unit) {
        try {
            val process = spawnProcess(arrayOf("sh", "-c", command))
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
