package com.hizv.shell.ui.screen

import android.net.Uri
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
import com.hizv.shell.shizuku.ShellRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var output by remember { mutableStateOf("选择一个 APK 文件进行安装") }
    var installing by remember { mutableStateOf(false) }

    val pickApk = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        installing = true
        output = "正在读取 APK…"
        scope.launch(Dispatchers.IO) {
            try {
                // 复制 APK 到缓存目录
                val tmpFile = File(context.cacheDir, "install.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val path = tmpFile.absolutePath
                scope.launch(Dispatchers.Main) { output = "正在安装 $path …" }
                // 通过 shizuku 执行 pm install
                val result = ShellRunner.exec("pm install -r -g $path")
                scope.launch(Dispatchers.Main) {
                    output = "安装结果:\n${result.output}\n[exit: ${result.exitCode}]"
                    installing = false
                    Toast.makeText(context, if (result.exitCode == 0) "安装成功" else "安装失败", Toast.LENGTH_SHORT).show()
                    tmpFile.delete()
                }
            } catch (e: Exception) {
                scope.launch(Dispatchers.Main) {
                    output = "错误: ${e.message}"
                    installing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("安装 APK") },
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
                Icons.Filled.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text("通过 Shizuku shell 静默安装 APK", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { pickApk.launch(arrayOf("application/vnd.android.package-archive")) },
                enabled = !installing
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (installing) "安装中…" else "选择 APK 文件")
            }
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    output,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
