package com.hizv.shell.ui.screen

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.hizv.shell.service.HizvDeviceAdminReceiver
import com.hizv.shell.shizuku.ShellRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAdmin by remember { mutableStateOf(checkDeviceAdmin(context)) }
    var installOutput by remember { mutableStateOf("") }
    var installing by remember { mutableStateOf(false) }

    val adminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isAdmin = checkDeviceAdmin(context) }

    val pickApk = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        installing = true
        installOutput = "正在读取 APK…"
        scope.launch(Dispatchers.IO) {
            try {
                val tmpFile = File(context.cacheDir, "install.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmpFile).use { out -> input.copyTo(out) }
                }
                val path = tmpFile.absolutePath
                installOutput = "正在安装…"
                val r = ShellRunner.exec("pm install -r -g $path")
                installOutput = if (r.exitCode == 0)
                    "安装成功" else "安装失败: ${r.output}"
                tmpFile.delete()
            } catch (e: Exception) {
                installOutput = "错误: ${e.message}"
            }
            installing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("设置", fontSize = 28.sp, fontWeight = FontWeight.Bold)

        // 设备管理员
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("设备管理员", fontWeight = FontWeight.Medium)
                        Text(
                            if (isAdmin) "已激活" else "未激活",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAdmin)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (!isAdmin) {
                    Button(onClick = {
                        val component = ComponentName(context, HizvDeviceAdminReceiver::class.java)
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "HizvShell 需要设备管理员权限")
                        }
                        adminLauncher.launch(intent)
                    }) { Text("激活设备管理员") }
                } else {
                    OutlinedButton(onClick = {
                        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                        dpm.removeActiveAdmin(ComponentName(context, HizvDeviceAdminReceiver::class.java))
                        isAdmin = false
                    }) { Text("移除设备管理员") }
                }
            }
        }

        // 安装 APK
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SystemUpdate, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("安装 APK", fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { pickApk.launch(arrayOf("application/vnd.android.package-archive")) },
                    enabled = !installing
                ) {
                    Text(if (installing) "安装中…" else "选择 APK 文件")
                }
                if (installOutput.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(installOutput, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 更新检查
        val updateInfo = com.hizv.shell.shizuku.UpdateChecker.updateInfo
        var checkingUpdate by remember { mutableStateOf(false) }
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("检查更新", fontWeight = FontWeight.Medium)
                        Text(
                            "当前版本 v${com.hizv.shell.shizuku.UpdateChecker.CURRENT_VERSION}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                updateInfo?.let { info ->
                    if (info.isUpdateAvailable) {
                        Text(
                            "发现新版本 v${info.latestVersion}!",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = {
                            com.hizv.shell.shizuku.UpdateChecker.openDownload(context, info.downloadUrl)
                        }) { Text("前往下载") }
                    } else {
                        Text("已是最新版本", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        checkingUpdate = true
                        scope.launch(Dispatchers.IO) {
                            com.hizv.shell.shizuku.UpdateChecker.check()
                            checkingUpdate = false
                        }
                    },
                    enabled = !checkingUpdate
                ) {
                    Text(if (checkingUpdate) "检查中…" else "检查更新")
                }
            }
        }

        // 关于
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于 HizvShell", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("版本: v1.1.0", style = MaterialTheme.typography.bodySmall)
                Text("基于 Shizuku API 13.1.5", style = MaterialTheme.typography.bodySmall)
                Text("无需 Root 的 Shell 权限管理器", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun checkDeviceAdmin(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return dpm.isAdminActive(ComponentName(context, HizvDeviceAdminReceiver::class.java))
}
