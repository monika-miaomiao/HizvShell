package com.hizv.shell.adb

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket

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

    private val client = AdbTcpClient()

    val isConnected: Boolean get() = state == State.CONNECTED && client.isConnected

    /**
     * 连接到 localhost:port。
     */
    fun connect(port: Int): Boolean {
        state = State.CONNECTING
        errorMsg = ""
        return try {
            val ok = client.connect("localhost", port)
            state = if (ok) State.CONNECTED else State.ERROR
            if (!ok) errorMsg = "连接失败，请确认无线调试已开启且端口正确"
            ok
        } catch (e: Exception) {
            state = State.ERROR
            errorMsg = e.message ?: "未知错误"
            false
        }
    }

    /**
     * 断开连接。
     */
    fun disconnect() {
        client.disconnect()
        state = State.IDLE
    }

    /**
     * 执行 shell 命令。
     */
    fun exec(command: String): String {
        if (!isConnected) return "错误: ADB 未连接"
        return try {
            client.exec(command)
        } catch (e: Exception) {
            state = State.ERROR
            errorMsg = e.message ?: "执行失败"
            "错误: ${e.message}"
        }
    }

}
