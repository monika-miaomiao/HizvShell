package com.hizv.shell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hizv.shell.adb.AdbManager
import com.hizv.shell.shizuku.ShizukuManager
import com.hizv.shell.ui.screen.*
import com.hizv.shell.ui.theme.HizvShellTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ShizukuManager.register()

        setContent {
            HizvShellTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 用 mutableState 观察连接状态变化
                    var connected by remember { mutableStateOf(AdbManager.isConnected) }

                    LaunchedEffect(Unit) {
                        // 轮询状态变化（简单方案）
                        while (true) {
                            kotlinx.coroutines.delay(500)
                            val now = AdbManager.isConnected
                            if (now != connected) connected = now
                        }
                    }

                    if (!connected) {
                        // 未连接：全屏激活页，没有底部导航
                        ActivationScreen(
                            onConnected = { connected = true }
                        )
                    } else {
                        // 已连接：带底部导航的主界面
                        val navController = rememberNavController()
                        val currentRoute = remember { mutableStateOf("home") }

                        Scaffold(
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = currentRoute.value == "home",
                                        onClick = {
                                            currentRoute.value = "home"
                                            navController.navigate("home") {
                                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = { Icon(Icons.Filled.Home, null) },
                                        label = { Text("主页") }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute.value == "terminal",
                                        onClick = {
                                            currentRoute.value = "terminal"
                                            navController.navigate("terminal") {
                                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = { Icon(Icons.Filled.Terminal, null) },
                                        label = { Text("终端") }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute.value == "apps",
                                        onClick = {
                                            currentRoute.value = "apps"
                                            navController.navigate("apps") {
                                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = { Icon(Icons.Filled.Apps, null) },
                                        label = { Text("应用") }
                                    )
                                    NavigationBarItem(
                                        selected = currentRoute.value == "settings",
                                        onClick = {
                                            currentRoute.value = "settings"
                                            navController.navigate("settings") {
                                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        },
                                        icon = { Icon(Icons.Filled.Settings, null) },
                                        label = { Text("设置") }
                                    )
                                }
                            }
                        ) { padding ->
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.padding(padding)
                            ) {
                                composable("home") { HomeScreen() }
                                composable("terminal") { TerminalScreen() }
                                composable("apps") { AppsScreen() }
                                composable("settings") { SettingsScreen() }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuManager.unregister()
    }
}
