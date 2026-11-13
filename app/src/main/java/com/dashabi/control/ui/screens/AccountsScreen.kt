package com.dashabi.control.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dashabi.control.AppContainer
import com.dashabi.control.data.Account
import com.dashabi.control.data.StaminaLevels
import com.dashabi.control.data.ApiResult
import com.dashabi.control.ui.AccountStatusFilters
import com.dashabi.control.ui.FacilityTimes
import com.dashabi.control.ui.accountStatusStyle
import com.dashabi.control.ui.clockOf
import com.dashabi.control.ui.formatOverdue
import com.dashabi.control.ui.components.ConfigureKeyCard
import com.dashabi.control.ui.components.ConfirmDialog
import com.dashabi.control.ui.components.DashaBottomBar
import com.dashabi.control.ui.components.DashaCard
import com.dashabi.control.ui.components.DashaTopBar
import com.dashabi.control.ui.components.EmptyState
import com.dashabi.control.ui.components.ErrorState
import com.dashabi.control.ui.components.InfoChip
import com.dashabi.control.ui.components.LoadingState
import com.dashabi.control.ui.components.StatusPill
import com.dashabi.control.ui.components.TinyAction
import com.dashabi.control.ui.extractMessage
import com.dashabi.control.ui.formatHafCoinWan
import com.dashabi.control.ui.formatWanAmount
import com.dashabi.control.ui.loginTypeLabel
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
import com.dashabi.control.data.CreditEntry
import com.dashabi.control.ui.parseCompactNumber
import com.dashabi.control.ui.vm.AccountSort
import com.dashabi.control.ui.vm.AccountsViewModel
import com.dashabi.control.ui.vm.rememberVm
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(
    container: AppContainer,
    refreshToken: Int,
    abnormalCount: Int,
    deviceAbnormalCount: Int,
    onOpenAccount: (Account) -> Unit,
    onOpenSettings: () -> Unit,
    onSelectTab: (Int) -> Unit
) {
    val vm = rememberVm(AccountsViewModel::class.java) {
        AccountsViewModel(container.repo, container.stamina, container.hafHistory, container.deviceOrder, container.cloudSync)
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val listState = rememberLazyListState()

    var searchText by rememberSaveable { mutableStateOf(vm.query) }
    var pendingSell by remember { mutableStateOf<Account?>(null) }
    var pendingPause by remember { mutableStateOf<Account?>(null) }
    val compactMode by container.uiPrefs.compactMode.collectAsState()
    val settings by container.settings.state.collectAsState()
    val staminaLevels by container.stamina.state.collectAsState()
    val credits by container.credit.state.collectAsState()
    val hafVersion by container.hafHistory.version.collectAsState()
    val deviceOrder by container.deviceOrder.order.collectAsState()
    var showHafPeak by remember { mutableStateOf(false) }
    var hafPeakLoading by remember { mutableStateOf(false) }
    var yesterdayPeakMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val gameNames by container.gameNames.state.collectAsState()
    val hasKey = settings.apiKey.isNotBlank()
    fun hafDeltaFor(a: Account): String? {
        val cur = parseCompactNumber(a.hafCoin) ?: return null
        val max = vm.hafMaxMap[a.accountCode]
        if (max == null) return container.hafHistory.deltaText(a.accountCode, a.hafCoin)
        val d = cur - max
        if (kotlin.math.abs(d) < 1.0) return "持平"
        return (if (d > 0) "+" else "-") + formatWanAmount(kotlin.math.abs(d))
    }

    val displayAccounts = remember(vm.visibleAccounts, vm.creditFilter, credits, hafVersion) {
        vm.visibleAccounts.filter { account ->
            val score = credits[account.accountCode]?.score
            when (vm.creditFilter) {
                "all" -> true
                "none" -> score == null
                "normal" -> score != null && score >= 350
                else -> score != null && score < 350
            }
        }
    }

    LaunchedEffect(hasKey, refreshToken) {
        if (hasKey) vm.load(silent = vm.accounts.isNotEmpty())
    }
    LaunchedEffect(hasKey) {
        if (!hasKey) return@LaunchedEffect
        while (true) {
            delay(60_000)
            vm.load(silent = true)
        }
    }
    LaunchedEffect(searchText) {
        if (!hasKey) return@LaunchedEffect
        if (searchText.trim() == vm.query) return@LaunchedEffect
        delay(150)
        if (searchText.trim() != vm.query) vm.setSearch(searchText)
    }
    // 切换设备 / 状态 / 排序（或搜索词）时，列表回到顶部
    LaunchedEffect(vm.deviceFilter, vm.statusFilter, vm.creditFilter, vm.sort, vm.sortDesc, vm.query, deviceOrder) {
        if (displayAccounts.isNotEmpty() &&
            (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
        ) {
            listState.scrollToItem(0)
        }
    }

    fun togglePause(account: Account) {
        val device = account.deviceCode ?: return
        scope.launch {
            val result = if (account.paused) {
                container.repo.resume(device, account.accountCode)
            } else {
                container.repo.pause(device, account.accountCode)
            }
            when (result) {
                is ApiResult.Ok -> {
                    snackbar.showSnackbar(
                        extractMessage(result.value)
                            ?: if (account.paused) "已恢复账号" else "已暂停账号"
                    )
                    vm.refresh()
                }
                is ApiResult.Err -> snackbar.showSnackbar(result.message)
            }
        }
    }

    val openPeak: () -> Unit = {
        showHafPeak = true
        hafPeakLoading = true
        yesterdayPeakMap = emptyMap()
        scope.launch {
            val today6 = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 6)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val start = today6 - 24 * 3600_000L
            yesterdayPeakMap = container.cloudSync.hafMaxSince(start, today6)
            hafPeakLoading = false
        }
    }

    Scaffold(
        topBar = {
            DashaTopBar(
                title = "账号总览",
                subtitle = if (hasKey) {
                    "共 ${vm.accounts.size} 个账号 · 更新于 ${clockOf(vm.lastUpdated)}"
                } else {
                    "未配置 API Key"
                },
                loading = vm.loading,
                actions = {
                    IconButton(onClick = { vm.resetFiltersForRefresh(); vm.refresh() }) {
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
                selected = 2,
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
        if (showHafPeak) {
            val currentTotal = displayAccounts.sumOf { parseCompactNumber(it.hafCoin) ?: 0.0 }
            val peakTotal = displayAccounts.sumOf { yesterdayPeakMap[it.accountCode] ?: 0.0 }
            val hasPeak = yesterdayPeakMap.isNotEmpty()
            val delta = currentTotal - peakTotal
            val shape = RoundedCornerShape(26.dp)
            Dialog(
                onDismissRequest = { showHafPeak = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f))
                        .clickable { showHafPeak = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth(0.88f)
                            .clip(shape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Surface2.copy(alpha = 0.98f), Surface1.copy(alpha = 0.94f))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
                            .clickable { }
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "哈夫币总额对比",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showHafPeak = false }) {
                                Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = TextMuted)
                            }
                        }
                        Text(
                            formatWanAmount(currentTotal),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Mint
                        )
                        Text("当前总额", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(Modifier.height(16.dp))
                        if (hafPeakLoading) {
                            Text("正在读取昨日峰值…", color = TextMuted)
                        } else if (!hasPeak) {
                            Text("暂无昨日数据", color = TextMuted)
                        } else {
                            Text("昨日峰值总额 ${formatWanAmount(peakTotal)}", color = TextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                (if (delta > 0) "+" else "-") + formatWanAmount(kotlin.math.abs(delta)),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (delta > 0) Rose else Mint
                            )
                            Text("对比昨日峰值", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!hasKey) {
                Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 12.dp)) {
                    ConfigureKeyCard(onOpenSettings = onOpenSettings)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(top = 12.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "balance") {
                        AccountBalanceSummary(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            total = formatWanAmount(displayAccounts.sumOf { parseCompactNumber(it.hafCoin) ?: 0.0 }),
                            count = displayAccounts.size,
                            onClick = openPeak
                        )
                    }
                    item(key = "controls") {
                        AccountListControls(
                            advancedActive = vm.creditFilter != "all" || vm.deviceFilter != null,
                            search = {
                                SearchBar(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    onClear = { searchText = ""; vm.setSearch("") },
                                    onSearch = { vm.setSearch(searchText); focus.clearFocus() },
                                    onFocus = { vm.resetFiltersForSearch() },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            },
                            statusFilters = { StatusFilterRow(vm) },
                            advancedFilters = {
                                CreditFilterRow(vm.accounts, credits, vm.creditFilter) { vm.selectCredit(it) }
                                if (vm.deviceOptions.size > 1) DeviceFilterRow(vm)
                            },
                            sort = { SortRow(vm, compactMode) { container.uiPrefs.setCompactMode(!compactMode) } }
                        )
                    }
                    if (vm.loading && vm.accounts.isEmpty()) {
                        item(key = "loading") { LoadingState("正在获取账号…") }
                    }
                    vm.error?.let { message ->
                        item(key = "error") { ErrorState(message, onRetry = { vm.refresh() }, modifier = Modifier.padding(horizontal = 12.dp)) }
                    }
                    if (!vm.loading && displayAccounts.isEmpty() && vm.error == null) {
                        item(key = "empty") {
                            if (vm.query.isNotBlank()) {
                                EmptyState("没有匹配的账号", "换个关键词，或清空搜索条件再试")
                            } else {
                                EmptyState("还没有账号", "当前 API Key 下没有任何账号")
                            }
                        }
                    }
                    if (compactMode) {
                        items(
                            items = displayAccounts.chunked(2),
                            key = { row -> row.joinToString("|") { it.accountCode } }
                        ) { row ->
                            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { account ->
                                    CompactAccountCard(
                                        account = account,
                                        levels = staminaLevels[account.accountCode],
                                        creditScore = credits[account.accountCode]?.score,
                                        creditPrevious = credits[account.accountCode]?.previousScore,
                                        hafDelta = hafDeltaFor(account),
                                        annotation = vm.annotations[account.accountCode],
                                        gameName = gameNames[account.accountCode]?.name,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onOpenAccount(account) }
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(
                            items = displayAccounts,
                            key = { "${it.deviceCode}/${it.accountCode}" }
                        ) { account ->
                            AccountCard(
                                account = account,
                                levels = staminaLevels[account.accountCode],
                                creditScore = credits[account.accountCode]?.score,
                                creditPrevious = credits[account.accountCode]?.previousScore,
                                hafDelta = hafDeltaFor(account),
                                annotation = vm.annotations[account.accountCode],
                                gameName = gameNames[account.accountCode]?.name,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                onOpen = { onOpenAccount(account) },
                                onTogglePause = {
                                    if (account.paused) togglePause(account) else pendingPause = account
                                },
                                onSell = { pendingSell = account }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingSell?.let { account ->
        ConfirmDialog(
            title = "出售账号？",
            message = "将提交「${account.gameNick ?: account.accountCode}」的出售任务，出售后不可撤销。\n" +
                "仅当设备处于「等待下一轮」时才会受理。",
            confirmText = "确认出售",
            destructive = true,
            onConfirm = {
                val device = account.deviceCode
                pendingSell = null
                if (device != null) {
                    scope.launch {
                        when (val result = container.repo.sellOne(device, account.accountCode)) {
                            is ApiResult.Ok -> {
                                snackbar.showSnackbar(extractMessage(result.value) ?: "出售请求已受理，可在设备详情查看进度")
                                vm.refresh()
                            }
                            is ApiResult.Err -> snackbar.showSnackbar(result.message)
                        }
                    }
                }
            },
            onDismiss = { pendingSell = null }
        )
    }

    pendingPause?.let { account ->
        ConfirmDialog(
            title = "暂停账号？",
            message = "将暂停「${account.gameNick ?: account.accountCode}」，暂停后该账号会停止执行任务，可随时恢复。",
            confirmText = "确认暂停",
            onConfirm = {
                pendingPause = null
                togglePause(account)
            },
            onDismiss = { pendingPause = null }
        )
    }
}

@Composable
internal fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(48.dp)
            .onFocusChanged { if (it.isFocused) onFocus() },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
        cursorBrush = SolidColor(Amber),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Row(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Surface1)
                    .border(1.dp, Line, shape)
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(7.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            "搜索昵称 / 游戏 ID / 备注",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "清空",
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun StatusFilterRow(vm: AccountsViewModel) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AccountStatusFilters, key = { it.first }) { (key, label) ->
            FilterPill(
                label = label,
                selected = vm.statusFilter == key,
                count = vm.statusCount(key),
                onClick = { vm.selectStatus(key) }
            )
        }
    }
}

@Composable
private fun DeviceFilterRow(vm: AccountsViewModel) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all-devices") {
            FilterPill(
                label = "全部设备",
                selected = vm.deviceFilter == null,
                onClick = { vm.selectDevice(null) }
            )
        }
        items(vm.deviceOptions, key = { it.first }) { (code, name) ->
            FilterPill(
                label = name,
                selected = vm.deviceFilter == code,
                onClick = { vm.selectDevice(code) }
            )
        }
    }
}

@Composable
private fun SortRow(
    vm: AccountsViewModel,
    compact: Boolean,
    onToggleCompact: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "sort-label") {
                Text(
                    "排序",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            items(AccountSort.entries, key = { it.name }) { key ->
                SortChip(
                    label = key.label,
                    selected = vm.sort == key,
                    showArrow = vm.sort == key,
                    descending = vm.sortDesc,
                    onClick = { vm.selectSort(key) }
                )
            }
        }
        Box(
            Modifier
                .width(1.dp)
                .height(16.dp)
                .background(Line)
        )
        IconButton(
            onClick = onToggleCompact,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Icon(
                imageVector = if (compact) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                contentDescription = if (compact) "切换为标准模式" else "切换为缩略模式",
                tint = if (compact) Amber else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
internal fun SortChip(
    label: String,
    selected: Boolean,
    showArrow: Boolean,
    descending: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Amber.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (selected) Amber.copy(alpha = 0.3f) else Line.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Amber else TextSecondary
        )
        if (showArrow) {
            Spacer(Modifier.width(4.dp))
            Text(
                if (descending) "↓" else "↑",
                style = MaterialTheme.typography.labelMedium,
                color = Amber
            )
        }
    }
}

@Composable
internal fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    count: Int? = null
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Amber.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (selected) Amber.copy(alpha = 0.3f) else Line.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Amber else TextSecondary
        )
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = if (selected) Amber else TextMuted
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountCard(
    account: Account,
    levels: StaminaLevels?,
    creditScore: Int?,
    creditPrevious: Int? = null,
    hafDelta: String? = null,
    annotation: String? = null,
    gameName: String?,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onTogglePause: () -> Unit,
    onSell: () -> Unit
) {
    val style = accountStatusStyle(account)
    val remark = account.upstreamRemark?.trim()?.takeIf { it.isNotEmpty() }
    val displayGameName = gameName?.trim()?.takeIf { it.isNotEmpty() } ?: account.gameNick
    val overdue = if (account.paused) FacilityTimes.maxOverdue(account.facilities) else null
    DashaCard(modifier = modifier, accent = style.color, onClick = onOpen) {
        Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            remark ?: displayGameName ?: "未命名账号",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    StatusPill(style.label, style.color)
                }
                annotation?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        loginTypeLabel(account.loginType),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        account.accountCode,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(5.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    InfoChip(account.deviceName ?: "未知设备")
                    if (creditScore != null) {
                        val delta = creditPrevious?.let { creditScore - it } ?: 0
                        val change = if (delta > 0) " +$delta" else if (delta < 0) " $delta" else ""
                        InfoChip("信用 $creditScore$change", if (creditScore < 350) Rose else TextSecondary)
                    }
                    account.gameId?.takeIf { it.isNotBlank() }?.let { gameId ->
                        InfoChip("游戏 ID $gameId")
                    }
                    if (account.status == "device_stopped") InfoChip("设备已停止", Rose)
                    if (!account.websiteNote.isNullOrBlank()) InfoChip("网站备注", Amber)
                }
                if (overdue != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "已超时 ${formatOverdue(overdue)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Rose
                    )
                }
            }

        Spacer(Modifier.height(7.dp))

        Row(Modifier.fillMaxWidth()) {
            MiniStat("总资产", account.totalAsset ?: "—", TextPrimary, Modifier.weight(1f))
            MiniStat("哈夫币", formatHafCoinWan(account.hafCoin), Mint, Modifier.weight(1f))
            MiniStat("行动等级", account.actionLevel ?: "—", TextPrimary, Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Row {
            Text(
                "24h 峰值对比 ",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            val delta = hafDelta ?: "—"
            Text(
                delta,
                style = MaterialTheme.typography.labelSmall,
                color = when { delta.startsWith("+") -> Rose; delta.startsWith("-") -> Mint; else -> TextMuted }
            )
        }

        Spacer(Modifier.height(5.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoChip("体力 ${levels?.training?.let { "Lv$it" } ?: "—"}", Mint)
            InfoChip("负重 ${levels?.range?.let { "Lv$it" } ?: "—"}", Sky)
            InfoChip(
                levels?.awBullets?.let { "${it}发AW子弹" } ?: "AW子弹 —",
                Amber
            )
        }

        if (displayGameName != null && !remark.isNullOrBlank() && remark != displayGameName) {
            Spacer(Modifier.height(5.dp))
            Text(
                "游戏名 · ${displayGameName}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(5.dp))

        HorizontalDivider(color = Line.copy(alpha = 0.55f))
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TinyAction(
                icon = if (account.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                label = if (account.paused) "恢复" else "暂停",
                onClick = onTogglePause,
                modifier = Modifier.weight(1f),
                tint = if (account.paused) Mint else Amber
            )
            TinyAction(
                icon = Icons.Outlined.Sell,
                label = "出售",
                onClick = onSell,
                modifier = Modifier.weight(1f),
                tint = Rose
            )
            TinyAction(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                label = "详情",
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
/** 缩略模式下的双列账号卡片：只展示备注 / 总资产 / 哈夫币 / 行动等级 / 体力负重。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CompactAccountCard(
    account: Account,
    levels: StaminaLevels?,
    creditScore: Int?,
    creditPrevious: Int? = null,
    hafDelta: String? = null,
    annotation: String? = null,
    gameName: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val remark = account.upstreamRemark?.trim()?.takeIf { it.isNotEmpty() }
    val displayGameName = gameName?.trim()?.takeIf { it.isNotEmpty() } ?: account.gameNick
    val style = accountStatusStyle(account)
    DashaCard(
        modifier = modifier,
        accent = style.color,
        onClick = onClick,
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    remark ?: displayGameName ?: "未命名",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                annotation?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(4.dp))
                    Text(it, style = MaterialTheme.typography.labelSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            CompactStat("总资产", account.totalAsset ?: "—", TextPrimary, Modifier.weight(1f))
            CompactStat("哈夫币", formatHafCoinWan(account.hafCoin), Mint, Modifier.weight(1f))
        }
        Row {
            Text(
                "24h ",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            val delta = hafDelta ?: "—"
            Text(
                delta,
                style = MaterialTheme.typography.labelSmall,
                color = when { delta.startsWith("+") -> Rose; delta.startsWith("-") -> Mint; else -> TextMuted }
            )
        }
        Spacer(Modifier.height(6.dp))
        Row {
            CompactStat("行动等级", account.actionLevel ?: "—", Sky, Modifier.weight(1f))
            CompactStat("体力", levels?.training?.let { "Lv$it" } ?: "—", Mint, Modifier.weight(1f))
            CompactStat("负重", levels?.range?.let { "Lv$it" } ?: "—", Sky, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            InfoChip(style.label, style.color)
            if (creditScore != null) {
                val delta = creditPrevious?.let { creditScore - it } ?: 0
                val change = if (delta > 0) " +$delta" else if (delta < 0) " $delta" else ""
                InfoChip("信用 $creditScore$change", if (creditScore < 350) Rose else TextSecondary)
            }
            InfoChip(
                levels?.awBullets?.let { "${it}发AW子弹" } ?: "AW子弹 —",
                Amber
            )
            InfoChip(account.deviceName ?: "未知设备", TextSecondary)
        }
    }
}

@Composable
private fun CompactStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
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
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1
        )
    }
}
@Composable
private fun CreditFilterRow(
    accounts: List<Account>,
    credits: Map<String, CreditEntry>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "credit-all") {
            FilterPill(label = "全部信用", selected = selected == "all", onClick = { onSelect("all") }, count = accounts.size)
        }
        item(key = "credit-normal") {
            val count = accounts.count { credits[it.accountCode]?.score?.let { s -> s >= 350 } == true }
            FilterPill(label = "正常", selected = selected == "normal", onClick = { onSelect("normal") }, count = count)
        }
        item(key = "credit-low") {
            val count = accounts.count { credits[it.accountCode]?.score?.let { s -> s < 350 } == true }
            FilterPill(label = "低分", selected = selected == "low", onClick = { onSelect("low") }, count = count)
        }
        item(key = "credit-none") {
            val count = accounts.count { credits[it.accountCode]?.score == null }
            FilterPill(label = "未登记", selected = selected == "none", onClick = { onSelect("none") }, count = count)
        }
    }
}

/** 当前筛选结果的资产概览，点击沿用原有的峰值对比操作。 */
@Composable
internal fun AccountBalanceSummary(total: String, count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DashaCard(modifier = modifier, onClick = onClick, containerColor = com.dashabi.control.ui.theme.HeroStart, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("哈夫币合计", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(total, color = Mint, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("当前筛选 · $count 个", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("查看昨日峰值对比", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}


/** 搜索、筛选、排序共用布局；界面预览与正式页面共享此组件。 */
@Composable
internal fun AccountListControls(
    advancedActive: Boolean,
    search: @Composable () -> Unit,
    statusFilters: @Composable () -> Unit,
    advancedFilters: @Composable () -> Unit,
    sort: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        search()
        statusFilters()
        Row(Modifier.fillMaxWidth().padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { sort() }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "收起筛选" else if (advancedActive) "更多筛选 · 已启用" else "更多筛选", color = Amber)
            }
        }
        AnimatedVisibility(visible = expanded) { Column { advancedFilters() } }
        HorizontalDivider(color = Line.copy(alpha = 0.65f))
    }
}
