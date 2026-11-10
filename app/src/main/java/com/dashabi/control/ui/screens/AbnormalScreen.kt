package com.dashabi.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dashabi.control.data.Account
import com.dashabi.control.data.AutoSellRecord
import com.dashabi.control.ui.accountStatusStyle
import com.dashabi.control.ui.clockOf
import com.dashabi.control.ui.components.DashaBottomBar
import com.dashabi.control.ui.components.DashaCard
import com.dashabi.control.ui.components.DashaTopBar
import com.dashabi.control.ui.components.EmptyState
import com.dashabi.control.ui.components.ErrorState
import com.dashabi.control.ui.components.InfoChip
import com.dashabi.control.ui.components.LoadingState
import com.dashabi.control.ui.components.StatusPill
import com.dashabi.control.ui.formatHafCoinWan
import com.dashabi.control.ui.formatTimestamp
import com.dashabi.control.ui.loginTypeLabel
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.Ink
import com.dashabi.control.ui.theme.Mint
import androidx.compose.ui.graphics.Color
import com.dashabi.control.ui.theme.Rose
import com.dashabi.control.ui.theme.Sky
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.TextPrimary
import com.dashabi.control.ui.theme.TextSecondary
import com.dashabi.control.ui.theme.Violet
import com.dashabi.control.ui.vm.AbnormalAccount
import com.dashabi.control.ui.vm.AbnormalViewModel

@Composable
fun AbnormalScreen(
    vm: AbnormalViewModel,
    abnormalCount: Int,
    deviceAbnormalCount: Int,
    sellRecords: Map<String, AutoSellRecord>,
    onOpenAccount: (Account) -> Unit,
    onOpenSettings: () -> Unit,
    onSelectTab: (Int) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.load(silent = vm.items.isNotEmpty()) }

    Scaffold(
        topBar = {
            DashaTopBar(
                title = "异常账号",
                subtitle = "共 ${vm.items.size} 个 · 更新于 ${clockOf(vm.lastUpdated)}",
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
                selected = 1,
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
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (vm.loading && vm.items.isEmpty()) {
                item(key = "loading") { LoadingState("正在检查异常账号…") }
            }
            vm.error?.let { message ->
                item(key = "error") { ErrorState(message, onRetry = { vm.load() }) }
            }
            if (!vm.loading && vm.items.isEmpty() && vm.error == null) {
                item(key = "empty") {
                    EmptyState(
                        "没有异常账号",
                        "哈夫币低于 100 万、设施进度缺少完成时间或云端异常时，会出现在这里"
                    )
                }
            }
            items(vm.items, key = { it.account.accountCode }) { item ->
                AbnormalAccountCard(
                    item = item,
                    sell = sellRecords[item.account.accountCode]
                ) { onOpenAccount(item.account) }
            }
            item(key = "criteria") { CriteriaCard() }
        }
    }
}

@Composable
private fun CriteriaCard() {
    DashaCard(accent = Amber, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = Amber,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("异常判定", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "云端状态异常 · raw_status 账号冻结 / 需要验证（蓝脸/人脸） · 哈夫币低于 100 万 · 设施进度缺少完成时间或超时 10 分钟以上（仅状态正常的账号）。" +
                "这里只做归类展示，不影响账号总览的「异常」筛选。",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbnormalAccountCard(
    item: AbnormalAccount,
    sell: AutoSellRecord?,
    onClick: () -> Unit
) {
    val account = item.account
    val remark = account.upstreamRemark?.trim()?.takeIf { it.isNotEmpty() }
    val statusStyle = accountStatusStyle(account)

    DashaCard(accent = Rose, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "异常",
                style = MaterialTheme.typography.labelSmall,
                color = Rose,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Rose.copy(alpha = 0.12f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                remark ?: account.gameNick ?: "未命名账号",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            StatusPill(statusStyle.label, statusStyle.color)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            "${loginTypeLabel(account.loginType)} · ${account.accountCode} · ${account.deviceName ?: "未知设备"}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(7.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item.reasons.forEach { reason ->
                InfoChip(reason, reasonColor(reason))
            }
        }
        Spacer(Modifier.height(9.dp))
        Row {
            AbnormalStat("总资产", account.totalAsset ?: "—", Amber, Modifier.weight(1f))
            AbnormalStat("哈夫币", formatHafCoinWan(account.hafCoin), Mint, Modifier.weight(1f))
            AbnormalStat("行动等级", account.actionLevel ?: "—", Sky, Modifier.weight(1f))
        }
        if (item.hafCoin != null && item.hafCoin < 1_000_000.0) {
            Spacer(Modifier.height(6.dp))
            Text(
                if (sell == null) {
                    "自动出售：等待下一个偶数整点"
                } else {
                    "自动出售：${formatTimestamp(sell.lastAttemptAt)} · ${sell.message}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    sell == null -> TextMuted
                    sell.success -> Mint
                    else -> Rose
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AbnormalStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

/** 每种异常原因用不同色相，避免橙/红相近看不清。 */
@Composable
private fun reasonColor(r: String): Color = when {
    r.contains("超时") -> Rose
    r.contains("云端异常") -> Violet
    r.contains("冻结") -> Sky
    r.contains("蓝脸") -> Violet
    r.contains("人脸") -> Sky
    r.contains("验证") -> Mint
    r.contains("哈夫币") -> Amber
    else -> TextMuted
}