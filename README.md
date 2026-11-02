<div align="center">

# 🐰 HizvShell

**无需 Root 的 Android Shell 权限管理器**

基于 [Shizuku](https://shizuku.rikka.app/) API，通过无线调试 / ADB 获取 shell 权限
界面风格参考 SukiSU Ultra · Material You 动态取色

<br>

![Platform](https://img.shields.io/badge/Android-8.0%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-blue?logo=jetpackcompose)
![Shizuku](https://img.shields.io/badge/Shizuku-API%2013.1-orange)
![License](https://img.shields.io/badge/License-MIT-red)

</div>

---

## ✨ 功能一览

| 功能 | 说明 |
|---|---|
| 🖥 **交互式 Shell** | 在手机上直接运行 ADB shell 命令，支持历史记录、快捷命令 |
| 📱 **应用管理** | 一键冻结 / 解冻 / 卸载任意应用（`pm disable-user` / `pm uninstall`） |
| 📦 **静默安装** | 通过 `pm install -r` 静默安装 APK |
| 🔐 **设备管理员** | 激活 DeviceAdminReceiver，获得锁屏等额外权限 |
| 🎨 **Material You** | Android 12+ 自动跟随壁纸动态取色 |
| 🔄 **自动更新** | 内置 GitHub Release 更新检查 |
| ⚡ **零 Root** | 仅需 Shizuku（无线调试启动即可） |

## 📸 界面预览

SukiSU Ultra 风格四栏底部导航：

```
┌─────────────────────────┐
│  HizvShell          ⏻   │
│                         │
│  ┌──────────┐ ┌───────┐ │
│  │ 工作中    │ │权限   │ │
│  │ <ADB>    │ │Shell  │ │
│  │ ✓        │ ├───────┤ │
│  │          │ │模式   │ │
│  │          │ │活跃   │ │
│  └──────────┘ └───────┘ │
│                         │
│  ┌─────────────────────┐│
│  │ 版本  v1.1.0        ││
│  │ 设备  Xiaomi 17 Ultra││
│  │ Android 15 API 35   ││
│  └─────────────────────┘│
│                         │
│  [主页] [终端] [应用] [设置]│
└─────────────────────────┘
```

## 🚀 快速开始

### 前置条件

1. 安装 [Shizuku](https://shizuku.rikka.app/download/)
2. Android 11+：开启 **开发者选项 → 无线调试**，在 Shizuku 中启动
3. Android 10 及以下：连接电脑执行
   ```bash
   adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
   ```

### 安装 HizvShell

从 [Releases](https://github.com/monika-miaomiao/HizvShell/releases) 下载最新 APK，安装后打开即可。

## 🛠 常用命令

HizvShell 终端支持完整的 ADB shell 环境：

```bash
# 冻结/解冻应用
pm disable-user --user 0 com.example.app
pm enable com.example.app

# 卸载（仅对当前用户）
pm uninstall --user 0 com.example.app

# 静默安装
pm install -r /sdcard/Download/app.apk

# 授予/撤销权限
pm grant com.example.app android.permission.CAMERA
pm revoke com.example.app android.permission.CAMERA

# 查看电池/分辨率/Wi-Fi
dumpsys battery
wm size
dumpsys wifi | grep mWifiInfo
```

## 🏗 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material3 (Dynamic Color)
- **权限**: Shizuku API 13.1.5
- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 35 (Android 15)
- **构建**: AGP 8.7.3 / Gradle 8.10.2

## 📁 项目结构

```
app/src/main/java/com/hizv/shell/
├── MainActivity.kt              # 单 Activity + 底部导航
├── HizvApp.kt                   # Application
├── shizuku/
│   ├── ShizukuManager.kt        # Shizuku 连接/权限管理
│   ├── ShellRunner.kt           # Shell 命令执行（含完整环境变量）
│   └── UpdateChecker.kt         # GitHub Release 更新检查
├── model/AppInfo.kt             # 应用数据模型
├── repository/AppRepository.kt  # 已装应用列表
├── service/
│   └── HizvDeviceAdminReceiver.kt  # 设备管理员
└── ui/
    ├── theme/Theme.kt           # Material You 动态主题
    └── screen/
        ├── HomeScreen.kt        # SukiSU 风格主页
        ├── TerminalScreen.kt    # 交互式终端
        ├── AppsScreen.kt        # 应用管理
        └── SettingsScreen.kt    # 设置/更新/设备管理员
```

## 📄 License

[MIT](./LICENSE)

## 🙏 Credits

- [Shizuku](https://github.com/RikkaApps/Shizuku) — Rikka
- UI 设计参考 [SukiSU Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra)
