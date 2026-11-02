package com.hizv.shell.adb

import android.content.Context

/**
 * 自管理 ADB 连接 — 不依赖 Shizuku。
 * 用户在系统无线调试开启后，填入端口号即可直连 localhost。
 */
object AdbManager {

    enum class State { IDLE, CONNECTING, CONNECTED, ERROR }

    @Volatile
    var state: State = State.IDLE
        private set

    @Volatile
    var errorMsg: String = ""
        private set

    private var client: AdbTcpClient? = null

    val isConnected: Boolean get() = state == State.CONNECTED && client?.isConnected == true

    /**
     * 初始化（必须在 Application.onCreate 或首次使用前调用）。
     */
    fun init(context: Context) {
        if (client == null) {
            client = AdbTcpClient(context.applicationContext)
        }
    }

    /**
     * 连接到 localhost:port。
     */
    fun connect(port: Int): Boolean {
        state = State.CONNECTING
        errorMsg = ""
        return try {
            val c = client ?: throw IllegalStateException("AdbManager.init() 未调用")
            val ok = c.connect("localhost", port)
            state = if (ok) State.CONNECTED else State.ERROR
            if (!ok) errorMsg = "连接失败，请确认无线调试已开启且端口正确"
            ok
        } catch (e: Exception) {
            state = State.ERROR
            errorMsg = e.message ?: "未知错误"
            false
        }
    }

    fun disconnect() {
        client?.disconnect()
        state = State.IDLE
    }

    fun exec(command: String): String {
        if (!isConnected) return "错误: ADB 未连接"
        return try {
            client!!.exec(command)
        } catch (e: Exception) {
            state = State.ERROR
            errorMsg = e.message ?: "执行失败"
            "错误: ${e.message}"
        }
    }
}
