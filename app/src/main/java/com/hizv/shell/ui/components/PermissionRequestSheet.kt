package com.hizv.shell.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 电源键风格权限请求弹窗。
 *
 * 从右侧（电源键方向）滑入，其余位置变暗。
 * 圆形滑块（应用图标）从下往上滑到顶 = 同意；点击空白处 = 拒绝。
 * 点击图标 = 窗口向左滑（音量键方向），原位留下详情框。
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

    // 弹窗整体水平偏移（从右侧滑入）
    val sheetOffsetX by animateIntAsState(
        targetValue = 0,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sheetX"
    )

    // 滑块垂直位置（0 = 底部，1 = 顶部=同意）
    var sliderProgress by remember { mutableStateOf(0f) }
    var sheetState by remember { mutableStateOf(0) } // 0=right, 1=left(detail)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDeny() })
            }
    ) {
        // 右侧弹窗面板
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(sheetOffsetX, 0) }
                .width(180.dp)
                .height(360.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { /* consume, don't dismiss */ })
                }
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        permissionText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))

                    // 滑块轨道
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(200.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // 圆形滑块（应用图标）
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        0,
                                        (-sliderProgress * 200f).dp.toPx().toInt()
                                    )
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
                                            } else {
                                                sliderProgress = 0f
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val maxDrag = with(density) { 200.dp.toPx() }
                                            sliderProgress = (sliderProgress - dragAmount.y / maxDrag)
                                                .coerceIn(0f, 1f)
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = {
                                        // 点击图标 → 滑到左边（音量键方向）显示详情
                                        sheetState = 1
                                    })
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (sliderProgress > 0.8f) "松开以同意" else "上滑授权",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 详情框（点击图标后出现在原位）
        if (sheetState == 1) {
            Card(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .width(180.dp)
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("$appName 请求权限", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(permissionText, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onDeny() }) { Text("拒绝") }
                        Button(onClick = { onAccept() }) { Text("同意") }
                    }
                }
            }
        }
    }
}
