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
    var step by remember { mutableStateOf(1) }
    var pairingPort by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var debugPort by remember { mutableStateOf("") }
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
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text("激活 HizvShell", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "无线调试直连，无需 Shizuku",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // === 步骤 1：配对 ===
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (step == 1)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("第 1 步：配对", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("1. 打开 开发者选项 → 无线调试", fontSize = 13.sp)
                Text("2. 点「使用配对码配对设备」", fontSize = 13.sp)
                Text("3. 记下页面显示的 端口号 和 6 位配对码", fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("打开开发者选项")
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pairingPort,
                        onValueChange = { pairingPort = it.filter { c -> c.isDigit() } },
                        label = { Text("配对端口") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { pairingCode = it.filter { c -> c.isDigit() } },
                        label = { Text("配对码") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pairingPort.length >= 4 && pairingCode.length == 6
                ) {
                    Text("配对码已确认 →")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // === 步骤 2：连接调试端口 ===
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (step == 2)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("第 2 步：连接", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("回到无线调试主页面（不要在配对页面）", fontSize = 13.sp)
                Text("记下 IP 地址下面显示的 端口号", fontSize = 13.sp)
                Text("（这个端口和配对端口不一样！）", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = debugPort,
                    onValueChange = { debugPort = it.filter { c -> c.isDigit() } },
                    label = { Text("调试端口") },
                    placeholder = { Text("例如: 41235") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val port = debugPort.toIntOrNull()
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
                    enabled = !connecting && debugPort.length >= 4
                ) {
                    if (connecting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("连接并激活")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // === 方式二：Shizuku 接管 ===
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("备用：通过 Shizuku 接管", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("如果你已用 AxManager / Shizuku 激活，", fontSize = 13.sp)
                Text("授权后自动杀进程接管。", fontSize = 13.sp)
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
                    OutlinedButton(
                        onClick = {
                            takeoverMsg = "正在接管…"
                            scope.launch(Dispatchers.IO) {
                                val knownForks = listOf(
                                    "moe.shizuku.privileged.api",
                                    "af.shizuku.plus.api",
                                    "com.rosan.dhizuku",
                                    "com.frb.axmanager",
                                )
                                val installedResult = ShellRunner.exec(
                                    "pm list packages | grep -iE 'shizuku|dhizuku|axmanager|shevery'"
                                )
                                val installedForks = installedResult.output.lines()
                                    .map { it.removePrefix("package:").trim() }
                                    .filter { it.isNotBlank() && it != context.packageName }
                                val allToKill = (knownForks + installedForks).distinct()
                                allToKill.forEach { pkg ->
                                    ShellRunner.exec("am force-stop $pkg")
                                }
                                val portResult = ShellRunner.exec("getprop service.adb.tcp.port")
                                val port = portResult.output.trim().toIntOrNull()
                                if (port != null && port > 0) {
                                    AdbManager.connect(port)
                                }
                                takeoverMsg = if (AdbManager.isConnected) {
                                    "接管成功！已关闭 ${allToKill.size} 个进程"
                                } else {
                                    "清理完成，但直连失败，请用第 2 步手动填端口"
                                }
                                if (AdbManager.isConnected) {
                                    scope.launch(Dispatchers.Main) { onConnected() }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("执行接管")
                    }
                }
                if (takeoverMsg.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(takeoverMsg, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // === Root 激活 ===
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("备用：Root 激活", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                var rootMsg by remember { mutableStateOf("") }
                Button(
                    onClick = {
                        rootMsg = "请求 root…"
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
                                    rootMsg = "Root 授权被拒绝"
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
