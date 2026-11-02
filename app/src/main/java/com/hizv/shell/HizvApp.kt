package com.hizv.shell

import android.app.Application
import com.hizv.shell.adb.AdbManager
import rikka.shizuku.Shizuku

class HizvApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AdbManager.init(this)
    }
}
