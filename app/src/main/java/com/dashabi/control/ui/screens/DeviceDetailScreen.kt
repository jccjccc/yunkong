package com.dashabi.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.dashabi.control.AppContainer
import com.dashabi.control.data.Account
import com.dashabi.control.data.Device
import com.dashabi.control.data.DeviceStatus
import com.dashabi.control.ui.accountStatusStyle
import com.dashabi.control.ui.components.ConfirmDialog
import com.dashabi.control.ui.components.DashaCard
import com.dashabi.control.ui.components.DashaTopBar
import com.dashabi.control.ui.components.EmptyState
import com.dashabi.control.ui.components.ErrorState
import com.dashabi.control.ui.components.InfoChip
import com.dashabi.control.ui.components.JsonFacts
import com.dashabi.control.ui.components.KeyValueRow
import com.dashabi.control.ui.components.LoadingState
import com.dashabi.control.ui.components.SectionTitle
import com.dashabi.control.ui.components.StatusDot
import com.dashabi.control.ui.components.StatusPill
import com.dashabi.control.ui.deviceOnlineLabel
import com.dashabi.control.ui.deviceStatusStyle
import com.dashabi.control.ui.deviceTimerLabel
import com.dashabi.control.ui.formatIsoFull
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.Ink
import com.dashabi.control.ui.theme.Line
import com.dashabi.control.ui.theme.Mint
import com.dashabi.control.ui.theme.Rose
import com.dashabi.control.ui.theme.Sky
import com.dashabi.control.ui.theme.Surface1
import com.dashabi.control.ui.theme.Surface2
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.TextPrimary
import com.dashabi.control.ui.theme.TextSecondary
import com.dashabi.control.ui.vm.DeviceDetailViewModel
import com.dashabi.control.ui.vm.rememberVm
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun DeviceDetailScreen(
    container: AppContainer,
    deviceCode: String,
    onBack: () -> Unit,
    onOpenAccount: (deviceCode: String, accountCode: String) -> Unit
) {
    val vm = rememberVm(DeviceDetailViewModel::class.java, key = deviceCode) { DeviceDetailViewModel(container.repo, deviceCode) }
    val snackbar = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var showStopDialog by remember { mutableStateOf(false) }
    var showBatchSellDialog by remember { mutableStateOf(false) }
    var sellExpanded by remember { mutableStateOf(false) }
    var disableTimerOnStop by remember { mutableStateOf(false) }
    val assistantVersions by container.assistantVersions.versions.collectAsState()
    val assistantLatest by container.assistantVersions.latest.collectAsState()
    val assistantWarnings by container.assistantVersions.warnings.collectAsState()
    var hafMaxMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    LaunchedEffect(deviceCode) {
        container.assistantVersions.refresh()
    }

    LaunchedEffect(Unit) {
        vm.load()
        vm.loadSellStatus()
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 6)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        hafMaxMap = container.cloudSync.hafMaxSince(cal.timeInMillis - 24 * 3600_000L, cal.timeInMillis)
    }

    fun hafDeltaFor(a: Account): String? {
        val cur = com.dashabi.control.ui.parseCompactNumber(a.hafCoin) ?: return null
        val max = hafMaxMap[a.accountCode]
        val fallback = container.hafHistory.deltaText(a.accountCode, a.hafCoin)
        if (max == null) return fallback
        val d = cur - max
        return if (kotlin.math.abs(d) < 1.0) "持平" else (if (d > 0) "+" else "-") + com.dashabi.control.ui.formatWanAmount(kotlin.math.abs(d))
    }
    // 账号列表变化时自动把 10 个 QQ 推给助手，助手内再勾选 TIM
    LaunchedEffect(vm.accounts) {
        val assistant = com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
        val all = vm.accounts.map { it.accountCode }
        if (assistant.isBlank() || all.isEmpty()) return@LaunchedEffect
        val key = all.joinToString(",")
        val prefs = ctx.getSharedPreferences("tim_sync", android.content.Context.MODE_PRIVATE)
        if (prefs.getString(deviceCode, "") == key) return@LaunchedEffect
        val ok = container.cloudSync.sendTimAccounts(assistant, all)
        if (ok) prefs.edit().putString(deviceCode, key).apply()
    }
    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it.text) }
    }

    Scaffold(
        topBar = {
            DashaTopBar(
                title = vm.device?.name ?: "设备详情",
                subtitle = vm.device?.model ?: deviceCode.take(10),
                onBack = onBack,
                loading = vm.loading,
                actions = {
                    IconButton(onClick = { vm.sync() }) {
                        Icon(Icons.Outlined.Sync, contentDescription = "同步账号", tint = TextSecondary)
                    }
                    IconButton(onClick = { vm.load() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = TextSecondary)
                    }
                }
            )
        },
        bottomBar = {
            if (vm.selected.isNotEmpty()) {
                Surface(color = Surface1, shadowElevation = 12.dp) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "已选择 ${vm.selected.size} 个账号",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                "批量出售要求设备处于等待下一轮",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        TextButton(onClick = { vm.clearSelection() }) {
                            Text("清空", color = TextSecondary)
                        }
                        Spacer(Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = {
                                val ids = vm.selected.mapNotNull { it.toLongOrNull() }
                                val cal = java.util.Calendar.getInstance()
                                android.app.TimePickerDialog(
                                    ctx,
                                    { _, hour, minute ->
                                        val at = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.HOUR_OF_DAY, hour)
                                            set(java.util.Calendar.MINUTE, minute)
                                            set(java.util.Calendar.SECOND, 0)
                                        }.timeInMillis
                                        val target = if (at <= System.currentTimeMillis()) at + 24 * 3600_000L else at
                                        val assistant = com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
                                        scope.launch {
                                            val ok = assistant.isNotBlank() &&
                                                container.cloudSync.scheduleSell(assistant, deviceCode, ids, target)
                                            snackbar.showSnackbar(
                                                if (ok) "服务器已接收：到点出售 ${ids.size} 个账号"
                                                else "定时失败，先绑定助手并检查安全令牌"
                                            )
                                        }
                                    },
                                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                                    cal.get(java.util.Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("定时出售", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = { showBatchSellDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Rose,
                                contentColor = Color(0xFF1B0505)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("批量出售", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Ink
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "status") {
                DeviceStatusCard(
                    device = vm.device,
                    live = vm.liveStatus,
                    onRefreshLive = { vm.refreshLive() }
                )
            }

            vm.error?.let { message ->
                item(key = "error") {
                    ErrorState(message, onRetry = { vm.load() })
                }
            }

            assistantWarnings[deviceCode]?.let { warning ->
                item(key = "assistant-warning") {
                    Text(
                        warning,
                        color = Rose,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item(key = "control-title") { SectionTitle("设备控制") }
            item(key = "controls") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ControlButton(
                            icon = Icons.Outlined.Timer,
                            title = "开启定时",
                            subtitle = "timer/enable",
                            tint = Mint,
                            enabled = vm.busyAction == null,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.toggleTimer(true) }
                        )
                        ControlButton(
                            icon = Icons.Outlined.TimerOff,
                            title = "关闭定时",
                            subtitle = "timer/disable",
                            tint = Amber,
                            enabled = vm.busyAction == null,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.toggleTimer(false) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ControlButton(
                            icon = Icons.Outlined.WbSunny,
                            title = "最低亮度",
                            subtitle = "远程调到 1%",
                            tint = TextSecondary,
                            enabled = vm.busyAction == null,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    val assistant = com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
                                    val ok = if (assistant.isBlank()) false else container.cloudSync.sendBrightness(assistant, 1)
                                    snackbar.showSnackbar(if (ok) "已通知助手调到最低亮度" else "下发失败，先绑定助手")
                                }
                            }
                        )
                        ControlButton(
                            icon = Icons.Outlined.WbSunny,
                            title = "50% 亮度",
                            subtitle = "远程调到 50%",
                            tint = Amber,
                            enabled = vm.busyAction == null,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                scope.launch {
                                    val assistant = com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
                                    val ok = if (assistant.isBlank()) false else container.cloudSync.sendBrightness(assistant, 50)
                                    snackbar.showSnackbar(if (ok) "已通知助手调到 50% 亮度" else "下发失败，先绑定助手")
                                }
                            }
                        )
                    }
                    ControlButton(
                        icon = Icons.Outlined.Sync,
                        title = "同步 TIM 账号",
                        subtitle = "把设备上的 QQ 列表推给助手勾选",
                        tint = TextSecondary,
                        enabled = vm.busyAction == null && vm.accounts.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                val assistant = com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
                                val all = vm.accounts.map { it.accountCode }
                                val ok = if (assistant.isBlank() || all.isEmpty()) false
                                else container.cloudSync.sendTimAccounts(assistant, all)
                                snackbar.showSnackbar(
                                    if (ok) "已推送 ${all.size} 个账号，去助手设置里勾选 TIM"
                                    else "推送失败，先绑定助手"
                                )
                            }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ControlButton(
                            icon = Icons.Outlined.StopCircle,
                            title = "停止脚本",
                            subtitle = "可同时关定时",
                            tint = Rose,
                            enabled = vm.busyAction == null,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                disableTimerOnStop = false
                                showStopDialog = true
                            }
                        )
                        ControlButton(
                            icon = Icons.Outlined.Sync,
                            title = "同步账号",
                            subtitle = "sync",
                            tint = Sky,
                            enabled = vm.busyAction == null,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.sync() }
                        )
                    }
                }
            }

            if (vm.sellStatus != null || vm.sellPolling) {
            item(key = "reboot") {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                var target by androidx.compose.runtime.remember(deviceCode) {
                    androidx.compose.runtime.mutableStateOf(
                        com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
                    )
                }
                var bound by androidx.compose.runtime.remember(deviceCode) {
                    androidx.compose.runtime.mutableStateOf(
                        com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode) != null
                    )
                }
                var msg by androidx.compose.runtime.remember(deviceCode) {
                    androidx.compose.runtime.mutableStateOf<String?>(null)
                }
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                val cloudSync = androidx.compose.runtime.remember {
                    com.dashabi.control.data.CloudSync(ctx, container.stamina, container.credit, container.gameNames)
                }
                val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
                    val contents = result.contents?.trim().orEmpty()
                    if (contents.isNotEmpty()) {
                        target = contents
                        com.dashabi.control.remote.RebootBindStore.put(ctx, deviceCode, contents)
                        bound = true
                        msg = "扫码绑定成功"
                    } else {
                        msg = "没有扫到二维码内容"
                    }
                }
                DashaCard(accent = Sky) {
                    androidx.compose.material3.Text(
                        "绑定助手设备",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    androidx.compose.material3.Text(
                        "只支持扫码绑定，输入框已锁定",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = if (bound) maskDeviceCode(target) else target,
                            onValueChange = { },
                            label = { androidx.compose.material3.Text("云控助手设备码") },
                            singleLine = true,
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        )
                        if (bound) {
                            androidx.compose.material3.OutlinedButton(onClick = {
                                val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                    as android.content.ClipboardManager
                                cm.setPrimaryClip(
                                    android.content.ClipData.newPlainText("云控助手设备码", target)
                                )
                                msg = "已复制设备码"
                            }) { androidx.compose.material3.Text("复制") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                scanLauncher.launch(
                                    ScanOptions().apply {
                                        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                        setPrompt("对准云控助手里的设备码二维码")
                                        setBeepEnabled(false)
                                        setOrientationLocked(false)
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) { androidx.compose.material3.Text("扫码绑定") }
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            enabled = bound,
                            onClick = {
                                com.dashabi.control.remote.RebootBindStore.remove(ctx, deviceCode)
                                target = ""
                                bound = false
                                msg = "已解绑"
                            },
                            modifier = Modifier.weight(1f)
                        ) { androidx.compose.material3.Text("解绑") }

                        androidx.compose.material3.OutlinedButton(
                            enabled = bound,
                            onClick = {
                                msg = "正在下发…"
                                scope.launch {
                                    val ok = runCatching {
                                        cloudSync.sendReboot(
                                            com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
                                        )
                                    }.getOrDefault(false)
                                    msg = if (ok) "重启指令已下发" else "下发失败"
                                }
                            },
                            modifier = Modifier.weight(1.35f)
                        ) { androidx.compose.material3.Text("远程重启") }
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedButton(
                        enabled = bound,
                        onClick = {
                            msg = "正在通知助手更新…"
                            scope.launch {
                                val assistant = com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
                                val ok = if (assistant.isBlank()) false else cloudSync.sendUpdate(assistant)
                                msg = if (ok) "已通知助手下载更新" else "下发失败，检查网络"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { androidx.compose.material3.Text("更新助手") }
                    if (bound) {
                        val ver = assistantVersions[deviceCode]?.takeIf { it.isNotBlank() }
                        val latest = assistantLatest
                        val outdated = ver != null && !latest.isNullOrBlank() && ver != latest
                        Spacer(Modifier.height(6.dp))
                        androidx.compose.material3.Text(
                            "助手版本 ${ver ?: "未上报"}" +
                                if (outdated) "（最新 $latest）" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (outdated) Rose else TextMuted
                        )
                    }
                    msg?.let {
                        Spacer(Modifier.height(6.dp))
                        androidx.compose.material3.Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

                item(key = "sell") {
                    DashaCard(accent = Amber) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { sellExpanded = !sellExpanded }
                        ) {
                            Text(
                                "出售任务",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            if (vm.sellPolling) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Amber,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "跟踪中",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Amber
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (sellExpanded) "收起 ▴" else "展开 ▾",
                                style = MaterialTheme.typography.labelMedium,
                                color = Amber
                            )
                        }
                        if (sellExpanded) {
                            Spacer(Modifier.height(8.dp))
                            JsonFacts(vm.sellStatus, emptyText = "暂无出售任务")
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        val ids = vm.selected.mapNotNull { it.toLongOrNull() }
                                        if (ids.isEmpty()) {
                                            scope.launch { snackbar.showSnackbar("先在设备账号里勾选要出售的账号") }
                                        } else {
                                            val cal = java.util.Calendar.getInstance()
                                            android.app.TimePickerDialog(
                                                ctx,
                                                { _, hour, minute ->
                                                    val at = java.util.Calendar.getInstance().apply {
                                                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                                                        set(java.util.Calendar.MINUTE, minute)
                                                        set(java.util.Calendar.SECOND, 0)
                                                    }.timeInMillis
                                                    val target = if (at <= System.currentTimeMillis()) at + 24 * 3600_000L else at
                                                    val assistant = com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
                                                    if (assistant.isBlank()) {
                                                        scope.launch { snackbar.showSnackbar("先绑定助手，服务器才能到点通知它出售") }
                                                    } else {
                                                        scope.launch {
                                                            val ok = container.cloudSync.scheduleSell(assistant, deviceCode, ids, target)
                                                            snackbar.showSnackbar(
                                                                if (ok) "服务器已接收：到点由助手出售 ${ids.size} 个账号"
                                                                else "定时提交失败，检查安全令牌和网络"
                                                            )
                                                        }
                                                    }
                                                },
                                                cal.get(java.util.Calendar.HOUR_OF_DAY),
                                                cal.get(java.util.Calendar.MINUTE),
                                                true
                                            ).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 0.dp)
                                ) {
                                    Text("定时出售", color = Rose, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.width(12.dp))
                                TextButton(
                                    onClick = { vm.loadSellStatus(true) },
                                    contentPadding = PaddingValues(horizontal = 0.dp)
                                ) {
                                    Text("刷新出售状态", color = Amber, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "accounts-title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(
                        "设备账号 ${vm.accounts.size}",
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { vm.selectAll() }) {
                        Text("全选", color = Amber, style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = {
                        val ids = vm.selected.mapNotNull { it.toLongOrNull() }
                        if (ids.isEmpty()) {
                            scope.launch { snackbar.showSnackbar("先勾选要定时出售的账号") }
                        } else {
                            val cal = java.util.Calendar.getInstance()
                            android.app.TimePickerDialog(
                                ctx,
                                { _, hour, minute ->
                                    val at = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                                        set(java.util.Calendar.MINUTE, minute)
                                        set(java.util.Calendar.SECOND, 0)
                                    }.timeInMillis
                                    val target = if (at <= System.currentTimeMillis()) at + 24 * 3600_000L else at
                                    val assistant = com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode).orEmpty()
                                    scope.launch {
                                        val ok = assistant.isNotBlank() &&
                                            container.cloudSync.scheduleSell(assistant, deviceCode, ids, target)
                                        snackbar.showSnackbar(if (ok) "服务器已接收定时出售" else "定时失败，检查绑定和令牌")
                                    }
                                },
                                cal.get(java.util.Calendar.HOUR_OF_DAY),
                                cal.get(java.util.Calendar.MINUTE),
                                true
                            ).show()
                        }
                    }) {
                        Text("定时出售", color = Rose, style = MaterialTheme.typography.labelMedium)
                    }
                    if (vm.selected.isNotEmpty()) {
                        TextButton(onClick = { vm.clearSelection() }) {
                            Text("清空", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (vm.loading && vm.accounts.isEmpty()) {
                item(key = "loading") { LoadingState("正在读取账号…") }
            } else if (vm.accounts.isEmpty()) {
                item(key = "empty") { EmptyState("设备上没有账号", "该设备暂未同步到账号数据") }
            }

            items(vm.accounts, key = { it.accountCode }) { account ->
                DeviceAccountRow(
                    account = account,
                    hafDelta = hafDeltaFor(account),
                    selected = account.accountCode in vm.selected,
                    onToggle = { vm.toggleSelection(account.accountCode) },
                    onOpen = { onOpenAccount(deviceCode, account.accountCode) }
                )
            }
        }
    }

    if (showStopDialog) {
        ConfirmDialog(
            title = "停止设备脚本？",
            message = "停止后当前任务会立即结束，设备需要重新开启定时才会继续执行。",
            confirmText = "停止脚本",
            destructive = true,
            onConfirm = {
                vm.stop(disableTimerOnStop)
                showStopDialog = false
            },
            onDismiss = { showStopDialog = false },
            extra = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = disableTimerOnStop,
                        onCheckedChange = { disableTimerOnStop = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Amber,
                            uncheckedColor = TextMuted
                        )
                    )
                    Text(
                        "同时关闭设备定时",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        )
    }

    if (showBatchSellDialog) {
        ConfirmDialog(
            title = "批量出售 ${vm.selected.size} 个账号？",
            message = "将向设备提交所选账号的出售任务，出售不可撤销。\n" +
                "服务端会逐个校验账号归属与暂停状态，仅设备等待下一轮时才会受理。",
            confirmText = "确认出售",
            destructive = true,
            onConfirm = {
                vm.sellSelected()
                showBatchSellDialog = false
            },
            onDismiss = { showBatchSellDialog = false }
        )
    }
}

@Composable
private fun DeviceStatusCard(
    device: Device?,
    live: DeviceStatus?,
    onRefreshLive: () -> Unit
) {
    val status = live ?: device?.status
    val style = deviceStatusStyle(status)
    DashaCard(accent = style.color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(style.color, size = 10.dp, pulsing = status?.running == true)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    device?.name ?: "未知设备",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    device?.model ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1
                )
            }
            StatusPill(style.label, style.color)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Surface2)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("任务", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(Modifier.width(8.dp))
            Text(
                status?.taskName?.takeIf { it.isNotBlank() } ?: "空闲",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (status?.running != true && !status?.error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Rose.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = Rose,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    status?.error.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Rose
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoChip("定时 ${deviceTimerLabel(status?.timerEnabled)}")
            InfoChip("在线 ${deviceOnlineLabel(status?.online)}")
            InfoChip("服务 ${deviceOnlineLabel(status?.serviceEnabled)}")
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Line)
        Spacer(Modifier.height(8.dp))

        var showCode by remember { mutableStateOf(false) }
        val rawCode = device?.deviceCode ?: "—"
        val masked = if (showCode) rawCode else "••••••••（点击显示）"
        KeyValueRow("设备编号", masked, mono = true)
        Text(
            if (showCode) "点此隐藏" else "点此显示完整设备编码",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.clickable { showCode = !showCode }
        )
        KeyValueRow("账号数量", device?.accountCount?.toString() ?: "—")
        KeyValueRow("标签", device?.tag)
        KeyValueRow("更新时间", formatIsoFull(device?.updatedAt))

        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onRefreshLive,
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            Text(
                "读取实时状态 (devices/{code}/status) →",
                color = Amber,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    DashaCard(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        onClick = if (enabled) onClick else null
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) tint else TextMuted.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) TextPrimary else TextMuted
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DeviceAccountRow(
    account: Account,
    hafDelta: String?,
    selected: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    val style = accountStatusStyle(account)
    DashaCard(
        accent = if (selected) Amber else null,
        onClick = onOpen,
        contentPadding = PaddingValues(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Amber,
                    uncheckedColor = TextMuted
                )
            )
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        account.gameNick ?: account.accountCode,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(style.label, style.color)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${account.gameId ?: "—"} · 资产 ${account.totalAsset ?: "—"} · 哈夫币 ${account.hafCoin ?: "—"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text("24h 峰值对比 ", style = MaterialTheme.typography.labelSmall, color = com.dashabi.control.ui.theme.Violet)
                    val delta = hafDelta ?: "—"
                    Text(
                        delta,
                        style = MaterialTheme.typography.labelSmall,
                        color = when { delta.startsWith("+") -> Rose; delta.startsWith("-") -> Mint; else -> TextMuted }
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** 绑定后只露出首尾各两位，中间用 **** 遮住。 */
private fun maskDeviceCode(code: String): String {
    val c = code.trim()
    if (c.length <= 4) return c
    return c.take(2) + "****" + c.takeLast(2)
}
