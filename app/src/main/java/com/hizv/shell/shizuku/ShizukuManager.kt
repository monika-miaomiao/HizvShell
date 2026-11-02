package com.hizv.shell.shizuku

import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import rikka.shizuku.Shizuku

/**
 * Shizuku 状态管理器（单例）。
 * 监听 binder 连接/断开与权限授予状态，供 Compose UI 观察。
 */
object ShizukuManager {

    enum class State {
        /** Shizuku 服务未运行（binder 未收到） */
        NOT_RUNNING,
        /** Binder 已收到但权限未授予 */
        NO_PERMISSION,
        /** 已连接且已授权 */
        READY
    }

    var state by mutableStateOf(State.NOT_RUNNING)
        private set

    /** 当前 UID：0=root, 2000=shell/adb */
    var currentUid by mutableStateOf(-1)
        private set

    private const val REQUEST_CODE_PERMISSION = 42

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        state = State.NOT_RUNNING
        currentUid = -1
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            refreshState()
        }

    fun register() {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        refreshState()
    }

    fun unregister() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
    }

    fun requestPermission() {
        if (state == State.NO_PERMISSION) {
            Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
        }
    }

    private fun refreshState() {
        if (!Shizuku.pingBinder()) {
            state = State.NOT_RUNNING
            currentUid = -1
            return
        }
        currentUid = try { Shizuku.getUid() } catch (_: Exception) { -1 }
        state = if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            State.READY
        } else {
            State.NO_PERMISSION
        }
    }
}
