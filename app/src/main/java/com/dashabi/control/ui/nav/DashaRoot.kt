package com.dashabi.control.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import com.dashabi.control.DashaApp
import com.dashabi.control.ui.screens.AbnormalScreen
import com.dashabi.control.ui.screens.AccountDetailScreen
import com.dashabi.control.ui.screens.AccountsScreen
import com.dashabi.control.ui.screens.DeviceDetailScreen
import com.dashabi.control.ui.screens.DevicesScreen
import com.dashabi.control.ui.screens.RecognitionDialog
import com.dashabi.control.ui.screens.SettingsScreen
import com.dashabi.control.ui.vm.AbnormalViewModel
import com.dashabi.control.ui.vm.AutoSellViewModel
import com.dashabi.control.ui.vm.DevicesViewModel
import com.dashabi.control.ui.vm.RecognitionViewModel
import com.dashabi.control.ui.vm.SyncViewModel
import com.dashabi.control.ui.vm.rememberVm
import com.dashabi.control.ui.theme.Ink

object Routes {
    const val MAIN = "main"
    const val DEVICE = "device/{deviceCode}"
    const val ACCOUNT = "account/{deviceCode}/{accountCode}"

    fun device(deviceCode: String) = "device/$deviceCode"
    fun account(deviceCode: String, accountCode: String) = "account/$deviceCode/$accountCode"
}

@Composable
fun DashaRoot(initialTab: Int = 0, tabRequest: Int = 0) {
    val context = LocalContext.current
    val container = (context.applicationContext as DashaApp).container
    val nav = rememberNavController()
    val settings by container.settings.state.collectAsState()

    var settingsVersion by remember { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }

    // 点击异常通知：回到主界面并切到「异常」页
    LaunchedEffect(tabRequest) {
        if (tabRequest > 0) {
            nav.popBackStack(Routes.MAIN, inclusive = false)
            tab = initialTab
        }
    }

    val devicesVm = rememberVm(DevicesViewModel::class.java) {
        DevicesViewModel(container.repo, container.notifier)
    }
    LaunchedEffect(Unit) { devicesVm.load() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            devicesVm.load(silent = true)
        }
    }
    LaunchedEffect(settingsVersion) {
        if (settingsVersion > 0) devicesVm.load(silent = true)
    }

    val abnormalVm = rememberVm(AbnormalViewModel::class.java) { AbnormalViewModel(container.repo, container.notifier, container.autoSell) }
    rememberVm(AutoSellViewModel::class.java) {
        AutoSellViewModel(container.repo, container.autoSell)
    }
    val sellRecords by container.autoSell.state.collectAsState()
    LaunchedEffect(Unit) { abnormalVm.load() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            abnormalVm.load(silent = true)
        }
    }
    LaunchedEffect(settingsVersion) {
        if (settingsVersion > 0) abnormalVm.load(silent = true)
    }

    // 打开 App 自动同步一次（体力/负重/AW/信用分 + 云控助手设备码绑定表）
    val autoSyncVm = rememberVm(SyncViewModel::class.java) {
        SyncViewModel(container.repo, container.stamina, container.credit)
    }
    LaunchedEffect(Unit) {
        if (settings.apiKey.isNotBlank()) {
            autoSyncVm.sync()
            // 电脑端是信用分数据源：打开 App 时拉取一次
            runCatching { container.cloudSync.sync() }
        }
    }

    val recognitionVm = rememberVm(RecognitionViewModel::class.java) {
        RecognitionViewModel(
            repo = container.repo,
            aiSettings = container.aiSettings,
            staminaStore = container.stamina,
            vision = container.vision
        )
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        NavHost(
            navController = nav,
            startDestination = Routes.MAIN,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Routes.MAIN) {
                val stateHolder = rememberSaveableStateHolder()

                BackHandler(enabled = tab != 0) { tab = 0 }

                stateHolder.SaveableStateProvider(tab) {
                    when (tab) {
                        0 -> DevicesScreen(
                            container = container,
                            vm = devicesVm,
                            abnormalCount = abnormalVm.items.size,
                            deviceAbnormalCount = devicesVm.abnormalCount,
                            onOpenDevice = { code -> nav.navigate(Routes.device(code)) },
                            onOpenSettings = { tab = 3 },
                            onSelectTab = { tab = it }
                        )

                        1 -> AbnormalScreen(
                            vm = abnormalVm,
                            abnormalCount = abnormalVm.items.size,
                            deviceAbnormalCount = devicesVm.abnormalCount,
                            sellRecords = sellRecords,
                            onOpenAccount = { account ->
                                account.deviceCode?.let { device ->
                                    nav.navigate(Routes.account(device, account.accountCode))
                                }
                            },
                            onOpenSettings = { tab = 3 },
                            onSelectTab = { tab = it }
                        )

                        3 -> SettingsScreen(
                            container = container,
                            onDismiss = { tab = 0 },
                            onSaved = { settingsVersion++ },
                            onStartRecognition = { onlyMissing, mode -> recognitionVm.start(onlyMissing, mode) },
                            onSelectTab = { tab = it },
                            abnormalCount = abnormalVm.items.size,
                            deviceAbnormalCount = devicesVm.abnormalCount
                        )

                        else -> AccountsScreen(
                            container = container,
                            refreshToken = settingsVersion,
                            abnormalCount = abnormalVm.items.size,
                            deviceAbnormalCount = devicesVm.abnormalCount,
                            onOpenAccount = { account ->
                                account.deviceCode?.let { device ->
                                    nav.navigate(Routes.account(device, account.accountCode))
                                }
                            },
                            onOpenSettings = { tab = 3 },
                            onSelectTab = { tab = it }
                        )
                    }
                }
            }

            composable(
                route = Routes.DEVICE,
                arguments = listOf(navArgument("deviceCode") { type = NavType.StringType })
            ) { entry ->
                DeviceDetailScreen(
                    container = container,
                    deviceCode = entry.arguments?.getString("deviceCode").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenAccount = { device, account ->
                        nav.navigate(Routes.account(device, account))
                    }
                )
            }

            composable(
                route = Routes.ACCOUNT,
                arguments = listOf(
                    navArgument("deviceCode") { type = NavType.StringType },
                    navArgument("accountCode") { type = NavType.StringType }
                )
            ) { entry ->
                AccountDetailScreen(
                    container = container,
                    deviceCode = entry.arguments?.getString("deviceCode").orEmpty(),
                    accountCode = entry.arguments?.getString("accountCode").orEmpty(),
                    onBack = { nav.popBackStack() }
                )
            }
        }

        if (recognitionVm.running || recognitionVm.finished) {
            RecognitionDialog(vm = recognitionVm, onDismiss = {})
        }
    }
}
