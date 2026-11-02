package com.hizv.shell.ui.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizv.shell.shizuku.ShizukuManager

@Composable
fun ActivationScreen() {
    val context = LocalContext.current
    val state = ShizukuManager.state
    var step by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("激活 HizvShell", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "通过无线调试在本机激活 shell 权限，无需电脑",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // 步骤 1
        ActivationStep(
            number = 1,
            title = "开启无线调试",
            description = "前往开发者选项，开启「无线调试」",
            done = step >= 1,
            actionLabel = "打开无线调试设置"
        ) {
            try {
                context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            } catch (_: Exception) {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            }
            step = maxOf(step, 1)
        }

        Spacer(Modifier.height(12.dp))

        // 步骤 2
        ActivationStep(
            number = 2,
            title = "打开配对码页面",
            description = "在无线调试中点击「使用配对码配对设备」，保持页面打开",
            done = step >= 2,
            actionLabel = "打开配对页面"
        ) {
            try {
                val intent = Intent("com.android.settings.action.WIRELESS_DEBUG_PAIRING")
                context.startActivity(intent)
            } catch (_: Exception) {
                context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
            step = maxOf(step, 2)
        }

        Spacer(Modifier.height(12.dp))

        // 步骤 3
        ActivationStep(
            number = 3,
            title = "启动 Shizuku 服务",
            description = if (Build.VERSION.SDK_INT >= 31)
                "在 Shizuku app 中点击「通过无线调试启动」，或在通知栏填入配对码"
            else "连接电脑执行: adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
            done = step >= 3,
            actionLabel = "打开 Shizuku"
        ) {
            try {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (intent != null) {
                    context.startActivity(intent)
                } else {
                    // Shizuku 未安装，跳转到下载页
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                    )
                }
            } catch (_: Exception) {}
            step = maxOf(step, 3)
        }

        Spacer(Modifier.height(12.dp))

        // 步骤 4
        ActivationStep(
            number = 4,
            title = "授权 HizvShell",
            description = "返回本应用，点击下方按钮授予 Shizuku 权限",
            done = state == ShizukuManager.State.READY,
            actionLabel = when (state) {
                ShizukuManager.State.READY -> "已连接 ✓"
                ShizukuManager.State.NO_PERMISSION -> "点击授权"
                else -> "等待 Shizuku 运行…"
            }
        ) {
            ShizukuManager.requestPermission()
        }

        Spacer(Modifier.height(32.dp))

        // 状态
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = when (state) {
                    ShizukuManager.State.READY -> MaterialTheme.colorScheme.primaryContainer
                    ShizukuManager.State.NO_PERMISSION -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    when (state) {
                        ShizukuManager.State.READY -> "✓ 已连接"
                        ShizukuManager.State.NO_PERMISSION -> "Shizuku 运行中，待授权"
                        ShizukuManager.State.NOT_RUNNING -> "Shizuku 未运行"
                    },
                    fontWeight = FontWeight.Bold
                )
                if (state == ShizukuManager.State.READY) {
                    Spacer(Modifier.height(8.dp))
                    Text("权限: ${if (ShizukuManager.currentUid == 0) "Root" else "ADB Shell"}", fontSize = 14.sp)
                    Text("可以开始使用了！", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ActivationStep(
    number: Int,
    title: String,
    description: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (done)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (done) "✓" else "$number",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel)
            }
        }
    }
}