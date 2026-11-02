package com.hizv.shell.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hizv.shell.shizuku.ShellRunner
import com.hizv.shell.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class TermLine(val text: String, val isCmd: Boolean = false, val isErr: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen() {
    var input by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(listOf<TermLine>()) }
    var history by remember { mutableStateOf(listOf<String>()) }
    var histIdx by remember { mutableStateOf(-1) }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val presets = listOf(
        "id" to "显示当前 uid",
        "pm list packages -3" to "列出用户应用",
        "dumpsys battery" to "电池信息",
        "settings get global airplane_mode_on" to "飞行模式状态",
        "wm size" to "屏幕分辨率",
        "cat /proc/cpuinfo | head -5" to "CPU 信息",
        "ls /sdcard/" to "浏览 SD 卡"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shell 终端") },
                actions = {
                    IconButton(onClick = { lines = emptyList() }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "清空")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column {
                    // 快捷命令标签
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            onClick = {},
                            label = { Text("快捷:", fontSize = 11.sp) },
                            selected = false,
                            enabled = false
                        )
                        presets.take(4).forEach { (cmd, desc) ->
                            AssistChip(
                                onClick = { input = cmd },
                                label = { Text(desc, fontSize = 11.sp) }
                            )
                        }
                    }
                    // 输入栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "~$",
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("输入命令…") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions.Default,
                            trailingIcon = {
                                if (history.isNotEmpty()) {
                                    Row {
                                        IconButton(onClick = {
                                            if (histIdx < history.size - 1) {
                                                histIdx++
                                                input = history[history.size - 1 - histIdx]
                                            }
                                        }) {
                                            Icon(Icons.Filled.KeyboardArrowUp, "历史上一条")
                                        }
                                        IconButton(onClick = {
                                            if (histIdx > 0) {
                                                histIdx--
                                                input = history[history.size - 1 - histIdx]
                                            } else {
                                                histIdx = -1
                                                input = ""
                                            }
                                        }) {
                                            Icon(Icons.Filled.KeyboardArrowDown, "历史下一条")
                                        }
                                    }
                                }
                            }
                        )
                        FilledIconButton(
                            onClick = {
                                if (input.isNotBlank() && !running &&
                                    ShizukuManager.state == ShizukuManager.State.READY) {
                                    val cmd = input.trim()
                                    history = history + cmd
                                    histIdx = -1
                                    lines = lines + TermLine("~$ $cmd", isCmd = true)
                                    input = ""
                                    running = true
                                    scope.launch(Dispatchers.IO) {
                                        val result = ShellRunner.exec(cmd)
                                        scope.launch(Dispatchers.Main) {
                                            if (result.output.isNotBlank()) {
                                                result.output.split("\n").forEach { l ->
                                                    lines = lines + TermLine(l, isErr = result.exitCode != 0)
                                                }
                                            }
                                            lines = lines + TermLine("[exit ${result.exitCode}]")
                                            running = false
                                        }
                                    }
                                }
                            },
                            enabled = !running
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "执行")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (ShizukuManager.state != ShizukuManager.State.READY) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Shizuku 未连接", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "请先在主页启动 Shizuku 并授权",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(lines) { line ->
                        Text(
                            line.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = when {
                                line.isCmd -> MaterialTheme.colorScheme.primary
                                line.isErr -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    if (running) {
                        item {
                            Text("▋", fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                LaunchedEffect(lines.size) {
                    if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
                }
            }
        }
    }
}
