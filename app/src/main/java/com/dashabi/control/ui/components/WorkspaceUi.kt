package com.dashabi.control.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dashabi.control.ui.theme.*

internal enum class SettingsDestination(val title: String, val description: String, val icon: ImageVector) {
    OVERVIEW("设置", "服务连接与工作偏好", Icons.Outlined.Settings),
    CONNECTION("服务连接", "API 身份、服务地址与安全令牌", Icons.Outlined.Link),
    SYNC("数据同步", "电脑互通、设备码与授权状态", Icons.Outlined.Sync),
    APPEARANCE("外观设置", "深色、浅色与系统主题", Icons.Outlined.Palette),
    INTELLIGENCE("智能识别", "体力、负重与 AW 数据识别", Icons.Outlined.AutoAwesome),
    DEVICE_ORDER("设备管理", "账号筛选中的设备显示顺序", Icons.Outlined.Devices),
    UPDATE("版本更新", "客户端版本与更新安装", Icons.Outlined.SystemUpdate),
    HELP("使用帮助", "常用流程与操作说明", Icons.Outlined.HelpOutline)
}

@Composable
internal fun SettingsHub(
    identity: String,
    connection: String,
    version: String,
    onOpen: (SettingsDestination) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DashaCard(onClick = { onOpen(SettingsDestination.CONNECTION) }, containerColor = HeroStart, contentPadding = PaddingValues(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Amber.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Workspaces, null, tint = Amber)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("我的工作空间", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    Spacer(Modifier.height(2.dp))
                    Text(identity, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Line.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Text(connection, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        SettingsMenuGroup("工作空间", listOf(SettingsDestination.CONNECTION, SettingsDestination.SYNC, SettingsDestination.DEVICE_ORDER), onOpen)
        SettingsMenuGroup("体验与效率", listOf(SettingsDestination.APPEARANCE, SettingsDestination.INTELLIGENCE), onOpen)
        SettingsMenuGroup("支持与关于", listOf(SettingsDestination.UPDATE, SettingsDestination.HELP), onOpen)
        Text("三角洲云控  ·  v$version", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun SettingsMenuGroup(title: String, destinations: List<SettingsDestination>, onOpen: (SettingsDestination) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = TextMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp))
        DashaCard(contentPadding = PaddingValues(0.dp)) {
            destinations.forEachIndexed { index, item ->
                SettingsMenuRow(item.icon, item.title, item.description, { onOpen(item) })
                if (index < destinations.lastIndex) HorizontalDivider(Modifier.padding(start = 60.dp), color = Line.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
internal fun SettingsMenuRow(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Amber, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun PageSectionIntro(title: String, description: String, icon: ImageVector) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Amber.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Amber)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
internal fun DetailSectionTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Column(Modifier.weight(1f).selectable(selected == index, role = Role.Tab, onClick = { onSelect(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (selected == index) Amber else TextMuted,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Medium))
                    }
                    Box(Modifier.fillMaxWidth(0.54f).height(3.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (selected == index) Amber else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = Line.copy(alpha = 0.5f))
    }
}

@Composable
internal fun UpdatePanel(
    currentVersion: String,
    availableVersion: String?,
    notes: String,
    busy: Boolean,
    status: String?,
    onCheck: () -> Unit,
    onInstall: () -> Unit
) {
    DashaCard(contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.SystemUpdate, null, tint = Amber, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("云控客户端", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text("当前版本 v$currentVersion", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(if (availableVersion == null) "保持客户端更新" else "可更新至 v$availableVersion", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(if (availableVersion == null) "检查可用版本，获取功能改进与问题修复。" else notes.ifBlank { "此版本包含功能改进与问题修复。" }, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        if (busy) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Amber, trackColor = Surface3)
        }
        status?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = if (availableVersion == null) onCheck else onInstall, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
            Icon(if (availableVersion == null) Icons.Outlined.Refresh else Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (busy) "处理中…" else if (availableVersion == null) "检查更新" else "下载并安装更新")
        }
        if (availableVersion != null) {
            TextButton(onClick = onCheck, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("重新检查版本") }
        }
    }
}

@Composable
internal fun OperationFeedback(message: String, error: Boolean = false) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background((if (error) Rose else Amber).copy(alpha = 0.09f)).padding(14.dp), verticalAlignment = Alignment.Top) {
        Icon(if (error) Icons.Outlined.ErrorOutline else Icons.Outlined.Info, null, tint = if (error) Rose else Amber, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = if (error) Rose else TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}


@Composable
internal fun AssistantUpdatePanel(busy: Boolean, status: String?, onUpdate: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(busy, status) { if (busy || status != null) expanded = true }
    val visible = expanded || busy
    DashaCard(contentPadding = PaddingValues(0.dp)) {
        Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.SystemUpdateAlt, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("设备助手维护", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                if (busy) Text("正在发送更新通知", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            IconButton(onClick = { expanded = !expanded }, enabled = !busy, modifier = Modifier.size(44.dp)) {
                Icon(if (visible) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    if (visible) "收起助手维护" else "展开助手维护", tint = TextSecondary)
            }
        }
        AnimatedVisibility(visible) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = Line.copy(alpha = 0.5f))
                Text("仅通知已绑定助手且处于「等待下一轮」的设备更新。", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                OutlinedButton(onClick = onUpdate, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text(if (busy) "正在发送更新通知…" else "批量更新设备助手")
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Amber, trackColor = Surface3)
                status?.let { OperationFeedback(it) }
            }
        }
    }
}

@Composable
internal fun AttentionSummary(accounts: Int, devices: Int, loading: Boolean = false, failed: Boolean = false) {
    DashaCard(contentPadding = PaddingValues(12.dp)) {
        Text("待关注事项", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f)) {
                Text(accounts.toString(), color = if (accounts > 0) Rose else TextPrimary, style = MaterialTheme.typography.headlineSmall)
                Text("异常账号", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Column(Modifier.weight(1f)) {
                Text(devices.toString(), color = if (devices > 0) Rose else TextPrimary, style = MaterialTheme.typography.headlineSmall)
                Text("异常设备", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(when { loading -> "正在检查，请等待本次数据刷新。"; failed -> "本次刷新未完成，请重试后核对数据。"; else -> "账号按异常规则归类；设备异常详情请在设备列表查看。" }, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}


@Composable
internal fun AccountOperationsPanel(
    paused: Boolean,
    busyAction: String?,
    onTogglePause: () -> Unit,
    onProduction: () -> Unit,
    onSell: () -> Unit,
    onSchedule: () -> Unit
) {
    DashaCard(contentPadding = PaddingValues(12.dp)) {
        Text("账号操作", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("按需管理运行状态、生产任务与出售计划。", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountOperationButton(if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                if (paused) "恢复" else "暂停", if (paused) Mint else Amber, busyAction == null, Modifier.weight(1f), onTogglePause)
            AccountOperationButton(Icons.Outlined.Build, "制造", Sky, busyAction == null, Modifier.weight(1f), onProduction)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountOperationButton(Icons.Outlined.Sell, "出售", Rose, busyAction == null, Modifier.weight(1f), onSell)
            AccountOperationButton(Icons.Outlined.Schedule, "定时暂停", TextSecondary, busyAction == null, Modifier.weight(1f), onSchedule)
        }
        busyAction?.let {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Amber, trackColor = Surface3)
            Spacer(Modifier.height(8.dp))
            Text("正在提交操作，请稍候…", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AccountOperationButton(icon: ImageVector, label: String, tint: androidx.compose.ui.graphics.Color,
    enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
