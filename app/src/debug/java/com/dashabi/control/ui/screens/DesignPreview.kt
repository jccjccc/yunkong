package com.dashabi.control.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dashabi.control.data.*
import com.dashabi.control.ui.components.*
import com.dashabi.control.ui.theme.*

/** 仅供 Android Studio / 本地视觉测试使用，全部为虚构数据，无网络与设备控制。 */
@Composable
internal fun DesignPreview(accounts: Boolean = false, dark: Boolean = true, compact: Boolean = false) {
    DashaTheme(darkTheme = dark) {
        Scaffold(
            containerColor = Ink,
            topBar = {
                DashaTopBar(
                    if (accounts) "账号总览" else "设备总览",
                    "离线设计预览 · 示例数据",
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Outlined.Refresh, "刷新", tint = TextSecondary) }
                        IconButton(onClick = {}) { Icon(Icons.Outlined.Settings, "连接设置", tint = TextSecondary) }
                    }
                )
            },
            bottomBar = {
                DashaBottomBar(if (accounts) 2 else 0, Icons.Outlined.Devices, Icons.Outlined.Person,
                    Icons.Outlined.WarningAmber, 2, 1, {})
            }
        ) { insets ->
            LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accounts) {
                    item { AccountBalanceSummary("12,846.3W", 24, {}, Modifier.padding(horizontal = 12.dp)) }
                    item {
                        AccountListControls(false,
                            search = { SearchBar("", {}, {}, {}, {}, Modifier.padding(horizontal = 12.dp)) },
                            statusFilters = {
                                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item { FilterPill("全部", true, {}, 24) }
                                    item { FilterPill("正常", false, {}, 22) }
                                    item { FilterPill("暂停", false, {}, 2) }
                                    item { FilterPill("异常", false, {}, 0) }
                                }
                            },
                            advancedFilters = { Text("信用 · 全部    设备 · 全部", color = TextSecondary, modifier = Modifier.padding(20.dp)) },
                            sort = {
                                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item { SortChip("哈夫币", true, true, true, {}) }
                                    item { SortChip("体力", false, false, true, {}) }
                                    item { SortChip("AW", false, false, true, {}) }
                                    item { SortChip("行动等级", false, false, true, {}) }
                                }
                            }
                        )
                    }
                    if (compact) {
                        item {
                            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                repeat(2) { i ->
                                    CompactAccountCard(sampleAccount(i), null, 462, gameName = null,
                                        modifier = Modifier.weight(1f), onClick = {})
                                }
                            }
                        }
                    } else {
                        items(3) { i ->
                            AccountCard(sampleAccount(i), null, 462, hafDelta = "+128.6W", gameName = null,
                                modifier = Modifier.padding(horizontal = 12.dp), onOpen = {}, onTogglePause = {}, onSell = {})
                        }
                    }
                } else {
                    item { Box(Modifier.padding(horizontal = 12.dp)) { SummaryHero(Summary(devices = 12, accounts = 24, running = 9, waitingNextRound = 2, stopped = 1), 0) } }
                    item { Box(Modifier.padding(horizontal = 12.dp)) { AssistantUpdatePanel(false, null, {}) } }
                    item { SectionTitle("设备列表 · 12", Modifier.padding(horizontal = 12.dp)) }
                    items(4) { i ->
                        DeviceCard(Device(deviceCode = "preview-$i", name = listOf("主力设备 01", "备用设备 02", "设备名称较长的测试设备 03", "主力设备 04")[i],
                            model = "Android 14", accountCount = 4,
                            status = DeviceStatus(code = if (i == 1) "stopped" else "running", running = i != 1,
                                taskName = if (i == 1) "已停止" else "执行日常任务", timerEnabled = true)),
                            modifier = Modifier.padding(horizontal = 12.dp), assistantVersion = "1.2.0", onClick = {})
                    }
                }
            }
        }
    }
}

private fun sampleAccount(i: Int) = Account(accountCode = "1000000${i + 1}", deviceCode = "preview-$i",
    deviceName = "主力设备 01", loginType = "qq", gameNick = "三角洲行动员", gameId = "123456789",
    upstreamRemark = if (i == 0) "主力账号 · 日常托管" else "备用账号名称较长时的显示测试",
    status = "normal", totalAsset = "8,612W", hafCoin = "41223000", actionLevel = "60")

@Preview(name = "设备 · 深色", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun DevicesDarkPreview() = DesignPreview()
@Preview(name = "账号 · 深色", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun AccountsDarkPreview() = DesignPreview(accounts = true)
@Preview(name = "账号 · 浅色", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun AccountsLightPreview() = DesignPreview(accounts = true, dark = false)
@Preview(name = "缩略 · 窄屏", widthDp = 320, heightDp = 740, showBackground = true)
@Composable
private fun AccountsCompactPreview() = DesignPreview(accounts = true, compact = true)


@Preview(name = "设置 · 深色", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun SettingsDarkPreview() = WorkspaceSettingsPreview(true)

@Preview(name = "设置 · 浅色", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun SettingsLightPreview() = WorkspaceSettingsPreview(false)

@Composable
private fun WorkspaceSettingsPreview(dark: Boolean) {
    DashaTheme(dark) {
        Scaffold(containerColor = Ink, topBar = { DashaTopBar("设置", "服务连接与工作偏好") },
            bottomBar = { DashaBottomBar(3, Icons.Outlined.Devices, Icons.Outlined.Person, Icons.Outlined.WarningAmber, 2, 1, {}) }) { insets ->
            LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(12.dp)) {
                item { SettingsHub("示例运营工作空间", "离线设计预览 · 示例数据", "1.0", {}) }
            }
        }
    }
}


@Preview(name = "账号操作 · 窄屏", widthDp = 320, heightDp = 600, showBackground = true)
@Composable
private fun AccountOperationsPreview() {
    DashaTheme {
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp)) {
                AccountOperationsPanel(false, null, {}, {}, {}, {})
            }
        }
    }
}

@Preview(name = "AI 识别结果 · 窄屏", widthDp = 320, heightDp = 740, showBackground = true)
@Composable
private fun RecognitionResultPreview() {
    DashaTheme {
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp)) {
                DashaCard {
                    RecognitionProgressContent(false, com.dashabi.control.ui.vm.RecognizeMode.BOTH,
                        8, 8, 6, 2, "", "部分截图无法读取，请稍后重试。")
                }
            }
        }
    }
}
