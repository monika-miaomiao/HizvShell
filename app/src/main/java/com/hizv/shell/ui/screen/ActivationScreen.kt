package com.hizv.shell.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizv.shell.adb.AdbManager
import com.hizv.shell.shizuku.ShellRunner
import com.hizv.shell.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen(onConnected: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var portText by remember { mutableStateOf("") }
    var connecting by remember { mutableStateOf(false) }
    var takeoverMsg by remember { mutableStateOf("") }
    val state = AdbManager.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Icon(
            Icons.Filled.RocketLaunch,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("激活 HizvShell", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "选择激活方式",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // === 方式一：直连无线调试 ===
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("方式一：直连无线调试", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("1. 开发者选项 → 开启无线调试", fontSize = 13.sp)
                Text("2. 点进无线调试详情页看端口号", fontSize = 13.sp)
                Text("3. 填入端口号连接", fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            // 直接跳开发者选项里的无线调试
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                        } catch (_: Exception) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setClassName("com.android.settings", "com.android.settings.Settings\$DevelopmentSettingsActivity")
                                })
                            } catch (_: Exception) {}
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("打开无线调试设置")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter { c -> c.isDigit() } },
                    label = { Text("端口号") },
                    placeholder = { Text("例如: 37123") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val port = portText.toIntOrNull()
                        if (port != null && port in 1024..65535) {
                            connecting = true
                            scope.launch(Dispatchers.IO) {
                                val ok = AdbManager.connect(port)
                                connecting = false
                                if (ok) onConnected()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !connecting && portText.length >= 4
                ) {
                    if (connecting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("直连")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // === 方式二：通过 Shizuku 激活并接管 ===
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("方式二：通过 Shizuku 接管", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("需要先安装 Shizuku 并启动。", fontSize = 13.sp)
                Text("授权后 HizvShell 会执行命令获取持久权限，", fontSize = 13.sp)
                Text("然后杀掉 Shizuku 进程实现接管。", fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))

                val shizukuState = ShizukuManager.state
                Button(
                    onClick = { ShizukuManager.requestPermission() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = shizukuState == ShizukuManager.State.NO_PERMISSION
                ) {
                    Text(when (shizukuState) {
                        ShizukuManager.State.READY -> "Shizuku 已授权"
                        ShizukuManager.State.NO_PERMISSION -> "授权 Shizuku"
                        else -> "Shizuku 未运行"
                    })
                }

                if (shizukuState == ShizukuManager.State.READY) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            takeoverMsg = "正在接管…"
                            scope.launch(Dispatchers.IO) {
                                // 1. 获取持久 shell 权限
                                ShellRunner.exec("appops set ${context.packageName} android:read_device_identifiers allow")

                                // 2. 动态查询所有已安装的 Shizuku 变体
                                val knownForks = listOf(
                                    "moe.shizuku.privileged.api",   // Shizuku 官方
                                    "af.shizuku.plus.api",           // ShizukuPlus
                                    "com.rosan.dhizuku",             // Dhizuku
                                    "com.frb.axmanager",             // AxManager
                                    "io.github.vvb2060.shizuku",     // vvb2060 fork
                                )
                                // 运行时再用 pm list 扫描一遍，防止漏网
                                val installedResult = ShellRunner.exec("pm list packages | grep -iE 'shizuku|dhizuku|axmanager|shevery'")
                                val installedForks = installedResult.output.lines()
                                    .map { it.removePrefix("package:").trim() }
                                    .filter { it.isNotBlank() && it != context.packageName }
                                val allToKill = (knownForks + installedForks).distinct()

                                val killed = mutableListOf<String>()
                                allToKill.forEach { pkg ->
                                    ShellRunner.exec("am force-stop $pkg")
                                    killed.add(pkg)
                                }

                                // 3. 读取 ADB 端口并直连
                                val portResult = ShellRunner.exec("getprop service.adb.tcp.port")
                                val port = portResult.output.trim().toIntOrNull()
                                if (port != null && port > 0) {
                                    AdbManager.connect(port)
                                }
                                takeoverMsg = if (AdbManager.isConnected) {
                                    "接管成功！已关闭 ${killed.size} 个进程: ${killed.joinToString(", ")}"
                                } else {
                                    "已授权并清理 fork，但直连失败，请用方式一手动填端口"
                                }
                                if (AdbManager.isConnected) {
                                    scope.launch(Dispatchers.Main) { onConnected() }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("执行接管（杀 Shizuku）")
                    }
                }

                if (takeoverMsg.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(takeoverMsg, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // === 方式三：ADB 命令行激活（电脑端） ===
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("方式三：电脑 ADB 激活", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("用 USB 连接电脑，在 ADB 命令行执行：", fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "adb shell settings put global adb_enabled 1 && " +
                        "adb shell svc wifi enable",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("执行后回到方式一，端口会自动显示在无线调试页面。", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        // === 方式四：Root 激活 ===
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("方式四：Root 激活", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("如果你的设备已 Root，点击下方按钮直接获取 root shell。", fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                var rootMsg by remember { mutableStateOf("") }
                Button(
                    onClick = {
                        rootMsg = "正在请求 root…"
                        scope.launch(Dispatchers.IO) {
                            try {
                                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                                val output = process.inputStream.bufferedReader().readText()
                                val code = process.waitFor()
                                if (code == 0 && output.contains("uid=0")) {
                                    com.hizv.shell.shizuku.ShellRunner.rootMode = true
                                    rootMsg = "Root 授权成功！"
                                    scope.launch(Dispatchers.Main) { onConnected() }
                                } else {
                                    rootMsg = "Root 授权被拒绝或未 Root"
                                }
                            } catch (e: Exception) {
                                rootMsg = "设备未 Root"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("请求 Root")
                }
                if (rootMsg.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(rootMsg, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (state == AdbManager.State.ERROR) {
            Spacer(Modifier.height(16.dp))
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    AdbManager.errorMsg,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp
                )
            }
        }
    }
}
