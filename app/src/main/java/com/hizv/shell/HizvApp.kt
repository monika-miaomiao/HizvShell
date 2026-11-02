package com.hizv.shell

import android.app.Application
import rikka.shizuku.Shizuku

class HizvApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // ShizukuProvider 自动初始化 Sui，无需额外调用
    }
}
