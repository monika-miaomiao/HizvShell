package com.hizv.shell.ui.screen

import android.os.Build
import com.hizv.shell.shizuku.ShizukuManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun HomeScreen() {
    val state = ShizukuManager.state
    val uid = ShizukuManager.currentUid
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HizvShell",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { ShizukuManager.requestPermission() }) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = "刷新状态",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 状态卡片行：左边大卡片 + 右边两个小卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 大状态卡片
            val (cardBg, statusText, versionText, showCheck) = when (state) {
                ShizukuManager.State.READY -> Three(
                    MaterialTheme.colorScheme.primaryContainer,
                    "工作中",
                    if (uid == 0) "<ROOT>" else "<ADB>",
                    true
                )
                ShizukuManager.State.NO_PERMISSION -> Three(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    "待授权",
                    "<Shizuku 运行中>",
                    false
                )
                ShizukuManager.State.NOT_RUNNING -> Three(
                    MaterialTheme.colorScheme.surfaceVariant,
                    "未连接",
                    "<Shizuku 未启动>",
                    false
                )
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            statusText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            versionText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "版本: ${Build.VERSION.SDK_INT}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showCheck) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // 右侧两个小卡片
            Column(
                modifier = Modifier.width(120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("权限", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            when (state) {
                                ShizukuManager.State.READY -> if (uid == 0) "Root" else "Shell"
                                ShizukuManager.State.NO_PERMISSION -> "—"
                                ShizukuManager.State.NOT_RUNNING -> "无"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("模式", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            if (state == ShizukuManager.State.READY) "活跃" else "待机",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 信息列表卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                InfoRow("HizvShell 版本", "v1.0.0")
                InfoRow("Shizuku 状态", when (state) {
                    ShizukuManager.State.READY -> "已连接 (uid=$uid)"
                    ShizukuManager.State.NO_PERMISSION -> "运行中，待授权"
                    ShizukuManager.State.NOT_RUNNING -> "未运行"
                })
                InfoRow("设备型号", "${Build.MANUFACTURER} ${Build.MODEL}")
                InfoRow("Android 版本", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                InfoRow("SDK 版本", "Release ${Build.VERSION.RELEASE}")
                InfoRow("ABIs", Build.SUPPORTED_ABIS.take(3).joinToString(", "))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 快捷操作卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "快速操作",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                QuickActionRow(
                    icon = Icons.Filled.FlashOn,
                    label = "获取设备信息",
                    cmd = "getprop ro.product.model; getprop ro.build.version.release"
                )
                QuickActionRow(
                    icon = Icons.Filled.Refresh,
                    label = "重启 SystemUI",
                    cmd = "pkill -f com.android.systemui"
                )
                QuickActionRow(
                    icon = Icons.Filled.Wifi,
                    label = "查看 Wi-Fi 信息",
                    cmd = "dumpsys wifi | grep 'mWifiInfo'"
                )
                QuickActionRow(
                    icon = Icons.Filled.BatteryChargingFull,
                    label = "电池状态",
                    cmd = "dumpsys battery | head -10"
                )
            }
        }

        // 未连接时显示引导
        if (state == ShizukuManager.State.NOT_RUNNING) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("如何启动 Shizuku", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "1. 安装 Shizuku app (shizuku.rikka.app)",
                        "2. 开发者选项 → 开启无线调试",
                        "3. 在 Shizuku 中通过无线调试启动",
                        "4. 返回本应用，点右上角电源图标授权"
                    ).forEach { Text(it, fontSize = 13.sp) }
                }
            }
        }
    }
}

private data class Three<T>(val a: T, val b: String, val c: String, val d: Boolean)

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
}

@Composable
private fun QuickActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    cmd: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var result by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        TextButton(
            onClick = {
                running = true
                result = null
                Thread {
                    val r = com.hizv.shell.shizuku.ShellRunner.exec(cmd)
                    result = if (r.output.isNotBlank()) r.output else "[exit=${r.exitCode}]"
                    running = false
                }.start()
            },
            enabled = !running && ShizukuManager.state == ShizukuManager.State.READY
        ) {
            Text(if (running) "..." else "执行")
        }
    }
    result?.let {
        Text(
            it,
            fontSize = 12.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 32.dp, bottom = 4.dp)
        )
    }
}
