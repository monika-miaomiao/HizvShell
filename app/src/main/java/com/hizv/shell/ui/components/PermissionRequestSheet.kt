package com.hizv.shell.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 电源键风格权限请求弹窗。
 * - 右侧滑入，暗色遮罩
 * - 圆形滑块从下往上滑到顶 = 同意
 * - 长按音量+：滑块自动前进，1.5秒后同意
 * - 音量-：取消
 * - 点击空白处 = 拒绝
 */
@Composable
fun PermissionRequestSheet(
    appName: String,
    permissionText: String,
    onAccept: () -> Unit,
    onDeny: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var sliderProgress by remember { mutableStateOf(0f) }
    var sheetLeft by remember { mutableStateOf(false) }
    var volUpHeld by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // 长按音量+自动前进
    LaunchedEffect(volUpHeld) {
        if (volUpHeld) {
            val startTime = System.currentTimeMillis()
            while (volUpHeld && sliderProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                sliderProgress = (elapsed / 1500f).coerceAtMost(1f)
                if (elapsed >= 1500f) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAccept()
                    return@LaunchedEffect
                }
                delay(16)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                when {
                    event.key == Key.VolumeUp && event.type == KeyEventType.KeyDown -> {
                        volUpHeld = true
                        true
                    }
                    event.key == Key.VolumeUp && event.type == KeyEventType.KeyUp -> {
                        volUpHeld = false
                        sliderProgress = 0f
                        true
                    }
                    event.key == Key.VolumeDown -> {
                        onDeny()
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDeny() })
            }
    ) {
        // 右侧面板
        Card(
            modifier = Modifier
                .align(if (sheetLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 24.dp)
                .width(180.dp)
                .height(360.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = {}) },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Icon(Icons.Filled.Security, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text(appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(permissionText, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))

                // 滑块轨道
                Box(
                    modifier = Modifier
                        .width(56.dp).height(200.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(0, (-sliderProgress * 200f).dpToPx(density).roundToInt())
                            }
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        if (sliderProgress > 0.8f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onAccept()
                                        } else sliderProgress = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val maxPx = with(density) { 200.dp.toPx() }
                                        sliderProgress = (sliderProgress - dragAmount.y / maxPx)
                                            .coerceIn(0f, 1f)
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { sheetLeft = !sheetLeft })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Apps, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        volUpHeld -> "音量+长按中…"
                        sliderProgress > 0.8f -> "松开同意"
                        else -> "上滑授权 / 音量+"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 左侧详情框
        if (sheetLeft) {
            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 220.dp)
                    .width(180.dp).height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("$appName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(permissionText, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDeny) { Text("拒绝") }
                        Button(onClick = onAccept) { Text("同意") }
                    }
                }
            }
        }
    }
}

/**
 * 重启/断开连接控制滑条。
 * 上滑 = 重启连接，下滑 = 断开连接。
 */
@Composable
fun ConnectionControlSheet(
    onRestart: () -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    var progress by remember { mutableStateOf(0f) } // -1 = disconnect, +1 = restart

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(24.dp)
                .width(140.dp)
                .height(300.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("连接控制", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .width(48.dp).height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    // 上下箭头提示
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Refresh, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(40.dp))
                        Icon(Icons.Filled.PowerSettingsNew, null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(0, (-progress * 100f).dpToPx(density).roundToInt())
                            }
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (progress > 0.3f) MaterialTheme.colorScheme.primary
                                else if (progress < -0.3f) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outline
                            )
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        when {
                                            progress > 0.5f -> onRestart()
                                            progress < -0.5f -> onDisconnect()
                                        }
                                        progress = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val maxPx = with(density) { 100.dp.toPx() }
                                        progress = (progress - dragAmount.y / maxPx).coerceIn(-1f, 1f)
                                    }
                                )
                            }
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(if (progress > 0.3f) "松开重启"
                     else if (progress < -0.3f) "松开关闭"
                     else "上滑重启/下滑关闭",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun Float.dpToPx(density: androidx.compose.ui.unit.Density): Float =
    with(density) { this@dpToPx.dp.toPx() }
