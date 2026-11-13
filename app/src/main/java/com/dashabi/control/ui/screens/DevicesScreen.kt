package com.dashabi.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dashabi.control.AppContainer
import com.dashabi.control.data.Device
import com.dashabi.control.data.Summary
import com.dashabi.control.ui.clockOf
import com.dashabi.control.ui.components.ConfigureKeyCard
import com.dashabi.control.ui.components.DashaBottomBar
import com.dashabi.control.ui.components.DashaCard
import com.dashabi.control.ui.components.DashaTopBar
import com.dashabi.control.ui.components.EmptyState
import com.dashabi.control.ui.components.ErrorState
import com.dashabi.control.ui.components.InfoChip
import com.dashabi.control.ui.components.LoadingState
import com.dashabi.control.ui.components.SectionTitle
import com.dashabi.control.ui.components.StatBlock
import com.dashabi.control.ui.components.StatusDot
import com.dashabi.control.ui.components.StatusPill
import com.dashabi.control.ui.deviceStatusStyle
import com.dashabi.control.ui.deviceTimerLabel
import com.dashabi.control.ui.formatIso
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.Ink
import com.dashabi.control.ui.theme.HeroStart
import com.dashabi.control.ui.theme.Line
import com.dashabi.control.ui.theme.Mint
import com.dashabi.control.ui.theme.Rose
import com.dashabi.control.ui.theme.Sky
import com.dashabi.control.ui.theme.Surface1
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.TextPrimary
import com.dashabi.control.ui.theme.TextSecondary
import com.dashabi.control.ui.theme.Violet
import com.dashabi.control.ui.vm.DevicesViewModel
import com.dashabi.control.ui.vm.deviceAbnormalReasons
import com.dashabi.control.ui.naturalCompare
import com.dashabi.control.ui.vm.rememberVm
import kotlinx.coroutines.delay

@Composable
fun DevicesScreen(
    container: AppContainer,
    vm: DevicesViewModel,
    abnormalCount: Int,
    deviceAbnormalCount: Int,
    onOpenDevice: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSelectTab: (Int) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val settings by container.settings.state.collectAsState()
    val hasKey = settings.apiKey.isNotBlank()
    val assistantVersions by container.assistantVersions.versions.collectAsState()
    val assistantLatest by container.assistantVersions.latest.collectAsState()
    val assistantWarnings by container.assistantVersions.warnings.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var updatingAssistants by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    var compact by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        container.assistantVersions.refresh()
    }

    Scaffold(
        topBar = {
            DashaTopBar(
                title = "设备总览",
                subtitle = if (hasKey) {
                    "更新于 ${clockOf(vm.lastUpdated)} · 每 60 秒自动刷新"
                } else {
                    "未配置 API Key"
                },
                loading = vm.loading,
                actions = {
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = TextSecondary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "连接设置", tint = TextSecondary)
                    }
                }
            )
        },
        bottomBar = {
            DashaBottomBar(
                selected = 0,
                devicesIcon = Icons.Outlined.Devices,
                accountsIcon = Icons.Outlined.Person,
                abnormalIcon = Icons.Outlined.WarningAmber,
                abnormalCount = abnormalCount,
                deviceAbnormalCount = deviceAbnormalCount,
                onSelect = onSelectTab
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Ink
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!hasKey) {
                item(key = "missing-key") {
                    ConfigureKeyCard(
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                item(key = "hero") { SummaryHero(vm.summary, assistantWarnings.size) }
                item(key = "assistant-update") {
                    com.dashabi.control.ui.components.AssistantUpdatePanel(
                        busy = updatingAssistants, status = updateMessage,
                        onUpdate = {
                                val targets = vm.devices
                                    .filter { it.status?.code == "waiting_next_round" }
                                    .mapNotNull { d ->
                                        com.dashabi.control.remote.RebootBindStore.get(ctx, d.deviceCode)
                                            ?.let { assistant -> d to assistant }
                                    }
                                if (targets.isEmpty()) {
                                    updateMessage = "没有处于「等待下一轮」且已绑定助手的设备"
                                } else {
                                    updatingAssistants = true
                                    updateMessage = "正在通知 ${targets.size} 台助手更新…"
                                    scope.launch {
                                        try {
                                            var ok = 0
                                            targets.forEach { (_, assistant) ->
                                                if (container.cloudSync.sendUpdate(assistant)) ok++
                                            }
                                            updateMessage = "已通知 $ok/${targets.size} 台助手更新"
                                        } catch (e: Exception) {
                                            updateMessage = "通知失败，请稍后重试"
                                        } finally {
                                            updatingAssistants = false
                                        }
                                    }
                                }
                        }
                    )
                }
                item(key = "section") {
                    SectionTitle("设备列表") {
                        Text("${vm.devices.size} 台", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        IconButton(onClick = { compact = !compact }) {
                            Icon(if (compact) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                                contentDescription = if (compact) "切换为标准模式" else "切换为缩略模式",
                                tint = if (compact) Amber else TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                vm.error?.let { message ->
                    item(key = "error") { ErrorState(message, onRetry = { vm.load() }) }
                }
                if (vm.loading && vm.devices.isEmpty()) {
                    item(key = "loading") { LoadingState("正在获取设备…") }
                } else if (vm.devices.isEmpty() && vm.error == null) {
                    item(key = "empty") {
                        EmptyState("还没有设备", "当前 API Key 下未绑定任何设备")
                    }
                }
                val sortedDevices = vm.devices.sortedWith { a, b ->
                    val ra = deviceAbnormalReasons(a.status).isEmpty()
                    val rb = deviceAbnormalReasons(b.status).isEmpty()
                    when {
                        ra != rb -> if (ra) 1 else -1
                        else -> naturalCompare(a.name ?: a.deviceCode, b.name ?: b.deviceCode)
                    }
                }
                if (compact) {
                    items(sortedDevices.chunked(2), key = { row -> row.joinToString("|") { it.deviceCode } }) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { device ->
                                DeviceCard(
                                    device = device,
                                    assistantVersion = assistantVersions[device.deviceCode],
                                    assistantLatest = assistantLatest,
                                    assistantWarning = assistantWarnings[device.deviceCode],
                                    compact = true,
                                    modifier = Modifier.weight(1f)
                                ) { onOpenDevice(device.deviceCode) }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    items(sortedDevices, key = { it.deviceCode }) { device ->
                        DeviceCard(
                            device = device,
                            assistantVersion = assistantVersions[device.deviceCode],
                            assistantLatest = assistantLatest,
                            assistantWarning = assistantWarnings[device.deviceCode]
                        ) { onOpenDevice(device.deviceCode) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SummaryHero(summary: Summary?, assistantOffline: Int) {
    DashaCard(containerColor = HeroStart, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("运行总览", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Text("60 秒自动刷新", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverviewMetric(summary?.devices?.toString() ?: "—", "设备总数", TextPrimary, Modifier.weight(1.1f))
            OverviewMetric(summary?.accounts?.toString() ?: "—", "托管账号", TextPrimary, Modifier.weight(1f))
            OverviewMetric(summary?.running?.toString() ?: "—", "运行中", Mint, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Line.copy(alpha = 0.55f))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OverviewStatus("等待下一轮", summary?.waitingNextRound, Sky)
            OverviewStatus("已停止", summary?.stopped, Rose)
            OverviewStatus("助手无障碍断开", assistantOffline, Rose)
            if (summary?.unknown == null || summary.unknown > 0) OverviewStatus("未知", summary?.unknown, TextMuted)
        }
    }
}

@Composable
private fun OverviewMetric(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall,
            color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun OverviewStatus(label: String, count: Int?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(if (count != null && count > 0) color else TextMuted.copy(alpha = 0.5f)))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(count?.toString() ?: "—", style = MaterialTheme.typography.labelMedium, color = if (count != null && count > 0) color else TextMuted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DeviceCard(
    device: Device,
    assistantVersion: String? = null,
    assistantLatest: String? = null,
    assistantWarning: String? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val style = deviceStatusStyle(device.status)
    val abnormalReasons = deviceAbnormalReasons(device.status)
    val running = device.status?.running == true
    val ver = assistantVersion?.takeIf { it.isNotBlank() }
    val outdated = ver != null && !assistantLatest.isNullOrBlank() && ver != assistantLatest
    DashaCard(modifier = modifier, accent = if (assistantWarning != null || abnormalReasons.isNotEmpty()) Rose else style.color, onClick = onClick, contentPadding = if (compact) PaddingValues(10.dp) else PaddingValues(12.dp)) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(style.color, size = 9.dp, pulsing = running)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        device.name ?: device.deviceCode.take(8),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${device.accountCount} 个账号 · " +
                            (device.status?.taskName?.takeIf { it.isNotBlank() } ?: "空闲"),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(style.label, style.color)
            }
            return@DashaCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(style.color, size = 10.dp, pulsing = running)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    device.name ?: device.deviceCode.take(8),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${device.model ?: "未知机型"} · ${device.accountCount} 个账号",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (assistantWarning != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        assistantWarning,
                        style = MaterialTheme.typography.labelSmall,
                        color = Rose,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(style.label, style.color)
        }

        Spacer(Modifier.height(7.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "任务",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(Modifier.width(8.dp))
            Text(
                device.status?.taskName?.takeIf { it.isNotBlank() } ?: "空闲",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatIso(device.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        val additionalReasons = abnormalReasons.filterNot { it == style.label }
        if (additionalReasons.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                additionalReasons.joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = Rose
            )
        }

        if (device.status?.running != true && !device.status?.error.isNullOrBlank()) {
            Spacer(Modifier.height(9.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Rose.copy(alpha = 0.10f))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = Rose,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    device.status?.error.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Rose,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (ver != null || assistantLatest != null) {
                InfoChip("助手 ${ver ?: "未上报"}" + if (outdated) " → $assistantLatest" else "", if (outdated) Rose else TextMuted)
            }
            InfoChip("定时 ${deviceTimerLabel(device.status?.timerEnabled)}")
            if (!device.tag.isNullOrBlank()) InfoChip("标签 ${device.tag}")
            if (device.status?.online != null) {
                InfoChip(if (device.status?.online == true) "在线" else "离线")
            }
            InfoChip(device.deviceCode.take(6), TextMuted)
        }
    }
}
