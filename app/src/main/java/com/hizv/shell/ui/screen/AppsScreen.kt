package com.hizv.shell.ui.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hizv.shell.model.AppInfo
import com.hizv.shell.repository.AppRepository
import com.hizv.shell.shizuku.ShellRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var showSystem by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    LaunchedEffect(showSystem) {
        loading = true
        apps = AppRepository.getInstalledApps(context, includeSystem = showSystem)
        loading = false
    }

    val filtered = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用管理 (${filtered.size})") },
                actions = {
                    TextButton(onClick = { showSystem = !showSystem }) {
                        Text(if (showSystem) "系统应用" else "用户应用")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("搜索应用…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppRow(app = app, onAction = { selectedApp = app })
                    }
                }
            }
        }
    }

    selectedApp?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedApp = null },
            title = { Text(app.label) },
            text = {
                Column {
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("版本: ${app.versionName}", style = MaterialTheme.typography.bodySmall)
                    Text("UID: ${app.uid}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (app.isEnabled) "状态: 已启用" else "状态: 已冻结",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (app.isEnabled)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        val cmd = if (app.isEnabled)
                            "pm disable-user --user 0 ${app.packageName}"
                        else "pm enable ${app.packageName}"
                        scope.launch(Dispatchers.IO) {
                            val r = ShellRunner.exec(cmd)
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(context, r.output.ifBlank { "完成" }, Toast.LENGTH_SHORT).show()
                                apps = AppRepository.getInstalledApps(context, includeSystem = showSystem)
                            }
                        }
                        selectedApp = null
                    }) {
                        Text(if (app.isEnabled) "冻结" else "解冻")
                    }
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val r = ShellRunner.exec("pm uninstall --user 0 ${app.packageName}")
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(context, r.output.ifBlank { "完成" }, Toast.LENGTH_SHORT).show()
                                apps = AppRepository.getInstalledApps(context, includeSystem = showSystem)
                            }
                        }
                        selectedApp = null
                    }) {
                        Text("卸载", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { selectedApp = null }) { Text("关闭") }
                }
            }
        )
    }
}

@Composable
private fun AppRow(app: AppInfo, onAction: () -> Unit) {
    ListItem(
        headlineContent = { Text(app.label, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            app.icon?.let { icon ->
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        },
        trailingContent = {
            if (!app.isEnabled) {
                AssistChip(onClick = onAction, label = { Text("已冻结") })
            } else {
                IconButton(onClick = onAction) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "操作")
                }
            }
        },
        modifier = Modifier.clickable { onAction() }
    )
    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
}
