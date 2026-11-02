package com.hizv.shell.ui.screen

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hizv.shell.service.HizvDeviceAdminReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAdminScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isAdmin by remember { mutableStateOf(checkDeviceAdmin(context)) }

    val adminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        isAdmin = checkDeviceAdmin(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设备管理员") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isAdmin) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(24.dp))
            Text(
                if (isAdmin) "设备管理员已激活" else "设备管理员未激活",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "激活设备管理员后，HizvShell 可执行锁屏、恢复出厂设置等操作，" +
                "并为静默安装 APK 提供更高权限。",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(32.dp))

            if (!isAdmin) {
                Button(onClick = {
                    val component = ComponentName(context, HizvDeviceAdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "HizvShell 需要设备管理员权限以执行高级操作"
                        )
                    }
                    adminLauncher.launch(intent)
                }) {
                    Icon(Icons.Filled.Security, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("激活设备管理员")
                }
            } else {
                OutlinedButton(onClick = {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val component = ComponentName(context, HizvDeviceAdminReceiver::class.java)
                    dpm.removeActiveAdmin(component)
                    isAdmin = false
                    Toast.makeText(context, "已移除设备管理员", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("移除设备管理员")
                }
            }
        }
    }
}

private fun checkDeviceAdmin(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val component = ComponentName(context, HizvDeviceAdminReceiver::class.java)
    return dpm.isAdminActive(component)
}
