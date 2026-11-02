package com.hizv.shell.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var portText by remember { mutableStateOf("") }
    var connecting by remember { mutableStateOf(false) }
    val state by remember { derivedStateOf { AdbManager.state } }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            "无需安装 Shizuku，直连无线调试端口",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // 步骤说明
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("操作步骤", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("1. 打开 系统设置 → 开发者选项", fontSize = 13.sp)
                Text("2. 开启「无线调试」", fontSize = 13.sp)
                Text("3. 点击「无线调试」进入详情页", fontSize = 13.sp)
                Text("4. 找到 IP 地址和端口号", fontSize = 13.sp)
                Text("5. 只填入端口号（数字部分）", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("打开无线调试设置")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 端口输入
        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter { c -> c.isDigit() } },
            label = { Text("输入端口号") },
            placeholder = { Text("例如: 37123") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.PermDataSetting, null) }
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val port = portText.toIntOrNull()
                if (port != null && port in 1024..65535) {
                    connecting = true
                    scope.launch(Dispatchers.IO) {
                        AdbManager.connect(port)
                        connecting = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !connecting && portText.length >= 4
        ) {
            if (connecting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("连接", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state == AdbManager.State.ERROR) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    AdbManager.errorMsg,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp
                )
            }
        }
    }
}
