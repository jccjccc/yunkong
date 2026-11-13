package com.dashabi.control.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dashabi.control.AppContainer
import com.dashabi.control.data.Account
import com.dashabi.control.data.Facilities
import com.dashabi.control.data.Facility
import com.dashabi.control.data.ImageInfo
import com.dashabi.control.data.Images
import com.dashabi.control.ui.FacilityTimes
import com.dashabi.control.ui.accountStatusStyle
import com.dashabi.control.ui.components.ConfirmDialog
import com.dashabi.control.ui.components.DashaCard
import com.dashabi.control.ui.components.DetailSectionTabs
import com.dashabi.control.ui.components.PageSectionIntro
import com.dashabi.control.ui.components.OperationFeedback
import com.dashabi.control.ui.components.DashaTopBar
import com.dashabi.control.ui.components.EmptyState
import com.dashabi.control.ui.components.InfoChip
import com.dashabi.control.ui.components.KeyValueRow
import com.dashabi.control.ui.components.LoadingState
import com.dashabi.control.ui.components.SectionTitle
import com.dashabi.control.ui.components.StatBlock
import com.dashabi.control.ui.components.StatusPill
import com.dashabi.control.ui.components.ThumbImage
import com.dashabi.control.ui.formatHafCoinWan
import com.dashabi.control.ui.formatWanAmount
import com.dashabi.control.ui.parseCompactNumber
import com.dashabi.control.ui.formatOverdue
import com.dashabi.control.ui.formatTimestamp
import com.dashabi.control.ui.jsonString
import com.dashabi.control.ui.loginTypeLabel
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.Ink
import com.dashabi.control.ui.theme.Line
import com.dashabi.control.ui.theme.Mint
import com.dashabi.control.ui.theme.OnAccent
import com.dashabi.control.ui.theme.Rose
import com.dashabi.control.ui.theme.Sky
import com.dashabi.control.ui.theme.Surface1
import com.dashabi.control.ui.theme.Surface2
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.TextPrimary
import com.dashabi.control.ui.theme.TextSecondary
import com.dashabi.control.ui.vm.AccountDetailViewModel
import kotlinx.coroutines.launch
import com.dashabi.control.ui.vm.rememberVm
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class PreviewImage(val title: String, val url: String, val capturedAt: String?)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountDetailScreen(
    container: AppContainer,
    deviceCode: String,
    accountCode: String,
    onBack: () -> Unit
) {
    val vm = rememberVm(AccountDetailViewModel::class.java, key = "$deviceCode/$accountCode") {
        AccountDetailViewModel(container.repo, deviceCode, accountCode)
    }
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var preview by remember { mutableStateOf<PreviewImage?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf(false) }
    var pendingCancelId by remember { mutableStateOf<Long?>(null) }
    var metaExpanded by rememberSaveable { mutableStateOf(false) }
    val staminaLevels by container.stamina.state.collectAsState()
    val hafVersion by container.hafHistory.version.collectAsState()
    val levels = staminaLevels[accountCode]
    val credits by container.credit.state.collectAsState()
    val credit = credits[accountCode]
    val gameNames by container.gameNames.state.collectAsState()
    val syncedGameName = gameNames[accountCode]?.name
    var showCredit by remember { mutableStateOf(false) }
    var creditRescan by remember { mutableStateOf(false) }
    var hafRange by rememberSaveable { mutableStateOf(24L) }
    var annotationText by remember { mutableStateOf("") }
    var annotationSaving by remember { mutableStateOf(false) }
    var serverHafPoints by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }
    var serverHafMax by remember { mutableStateOf<Double?>(null) }
    var hafHover by remember { mutableStateOf<Pair<Long, Double>?>(null) }
    var hafHoverIndex by remember { mutableStateOf<Int?>(null) }
    var hafHoverX by remember { mutableStateOf(0f) }
    var hafChartWidth by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(accountCode) {
        annotationText = container.cloudSync.getAnnotations()[accountCode].orEmpty()
    }
    LaunchedEffect(vm.account?.hafCoin, hafVersion) {
        vm.account?.let { container.hafHistory.record(it.accountCode, it.hafCoin) }
    }
    LaunchedEffect(accountCode, vm.account?.hafCoin) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 6)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis > System.currentTimeMillis()) cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        serverHafMax = container.cloudSync.hafMaxSince(cal.timeInMillis - 24 * 3600_000L, cal.timeInMillis)[accountCode]
    }
    LaunchedEffect(hafRange, hafVersion) {
        serverHafPoints = container.cloudSync.hafHistory(
            accountCode,
            System.currentTimeMillis() - hafRange * 3600_000L
        )
    }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it.text) } }
    if (showCredit) {
        QqCreditDialog(
            container = container,
            accountCode = accountCode,
            rescan = creditRescan,
            onDismiss = { showCredit = false },
            onSaved = { score -> scope.launch { snackbar.showSnackbar("信用分 $score 已登记") } }
        )
    }

    fun previewUrl(kind: String): String? {
        val info: ImageInfo? = when (kind) {
            "profile" -> vm.images?.profile
            "tqc" -> vm.images?.tqc
            else -> vm.images?.store
        }
        if (info?.available != true) return null
        return container.repo.absoluteUrl(info.url) ?: container.repo.imageUrl(deviceCode, accountCode, kind)
    }

    var detailSection by androidx.compose.runtime.saveable.rememberSaveable(deviceCode, accountCode) { androidx.compose.runtime.mutableStateOf(0) }
    val detailListState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(detailSection) { detailListState.scrollToItem(0) }

    Scaffold(
        topBar = {
            Column {
            DashaTopBar(
                title = vm.account?.let { account ->
                    account.upstreamRemark?.trim()?.takeIf { it.isNotEmpty() }
                        ?: syncedGameName
                        ?: account.gameNick
                } ?: "账号详情",
                subtitle = vm.account?.deviceName ?: deviceCode.take(8),
                onBack = onBack,
                loading = vm.loading,
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = TextSecondary)
                    }
                }
            )
            DetailSectionTabs(listOf("概览", "资产", "操作", "资料"), detailSection) { detailSection = it }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Ink
    ) { padding ->
        if (vm.loading && vm.account == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingState("正在读取账号详情…") }
            return@Scaffold
        }
        if (vm.account == null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState("读取失败", vm.error ?: "账号不存在或无权访问")
            }
            return@Scaffold
        }

        val account = vm.account!!
        val hafDelta = parseCompactNumber(account.hafCoin)?.let { cur ->
            val max = serverHafMax
            if (max == null) return@let container.hafHistory.deltaText(account.accountCode, account.hafCoin)
            val d = cur - max
            if (kotlin.math.abs(d) < 1.0) "持平" else (if (d > 0) "+" else "-") + formatWanAmount(kotlin.math.abs(d))
        }
        val style = accountStatusStyle(account)
        val remark = account.upstreamRemark?.trim()?.takeIf { it.isNotEmpty() }

        LazyColumn(
            state = detailListState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (detailSection == 0) {
                item(key = "hero") {
                    DashaCard(accent = style.color) {
                        Column(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (remark != null) {
                                        Text(
                                            "备注",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Amber,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Amber.copy(alpha = 0.14f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        remark ?: syncedGameName ?: account.gameNick ?: "未命名账号",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    StatusPill(style.label, style.color)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(annotationText.ifBlank { "暂无标注 · 在资料页编辑" }, color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { detailSection = 3 }.padding(vertical = 8.dp))
                                Spacer(Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            clipboard.setText(AnnotatedString(account.accountCode))
                                            scope.launch { snackbar.showSnackbar("已复制 QQ 号") }
                                        }
                                ) {
                                    Text(
                                        "${loginTypeLabel(account.loginType)} · ${account.accountCode}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Outlined.ContentCopy,
                                        contentDescription = "复制 QQ 号",
                                        tint = TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                if (remark != null || syncedGameName != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "游戏名 · ${syncedGameName ?: account.gameNick ?: "—"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(7.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    InfoChip(account.deviceName ?: "未知设备", TextSecondary)
                                    InfoChip("游戏 ID ${account.gameId ?: "—"}", TextMuted)
                                    if (account.paused) InfoChip("暂停中", Amber)
                                }
                                if (account.paused) {
                                    val overdue = FacilityTimes.maxOverdue(account.facilities)
                                    if (overdue != null) {
                                        Spacer(Modifier.height(5.dp))
                                        Text(
                                            "已超时 ${formatOverdue(overdue)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Rose
                                        )
                                    }
                                }
                            }
                    }
                }
            }

            if (detailSection == 0) {
                item(key = "credit") {
                    DashaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("信用分", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        credit?.score?.toString() ?: "未登记",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (credit?.score != null && credit.score < 350) Rose else TextPrimary
                                    )
                                    if (credit?.score != null && credit.score < 350) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "低分",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Rose,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Rose.copy(alpha = 0.14f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            TextButton(
                                onClick = {
                                    if (credit == null) {
                                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                                        android.webkit.CookieManager.getInstance().flush()
                                    }
                                    creditRescan = false
                                    showCredit = true
                                }
                            ) { Text(if (credit == null) "扫码登记" else "更新", color = Amber) }
                            if (credit != null) {
                                TextButton(
                                    onClick = {
                                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                                        android.webkit.CookieManager.getInstance().flush()
                                        creditRescan = true
                                        showCredit = true
                                    }
                                ) { Text("重新扫码", color = TextMuted) }
                            }
                        }
                    }
                }
            }

            if (detailSection == 0 || detailSection == 1) {
                item(key = "assets") {
                    DashaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatBlock(
                                value = account.totalAsset ?: "—",
                                label = "总资产",
                                modifier = Modifier.weight(1f),
                                valueColor = Amber
                            )
                            StatBlock(
                                value = formatHafCoinWan(account.hafCoin),
                                label = "哈夫币",
                                modifier = Modifier.weight(1f),
                                valueColor = Mint
                            )
                            StatBlock(
                                value = account.actionLevel ?: "—",
                                label = "行动等级",
                                modifier = Modifier.weight(1f),
                                valueColor = Sky
                            )
                        }
                        if (hafDelta != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "24h 峰值对比 $hafDelta",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (hafDelta.startsWith("+")) Mint else Rose
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Line)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "资产更新于 ${account.assetUpdatedAt ?: "—"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            if (detailSection == 1) {
                item(key = "haf-chart") {
                    DashaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "哈夫币变化",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            listOf(24L to "24小时", 72L to "3天", 168L to "7天").forEach { (h, label) ->
                                TextButton(onClick = { hafRange = h }) {
                                    Text(
                                        label,
                                        color = if (hafRange == h) Mint else TextMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                        val now = System.currentTimeMillis()
                        val localPoints = container.hafHistory.points(accountCode, now - hafRange * 3600_000L)
                        val points = (if (serverHafPoints.isNotEmpty()) serverHafPoints else localPoints).toMutableList()
                        parseCompactNumber(account.hafCoin)?.let { cur ->
                            if (points.isEmpty() || points.last().second != cur) points.add(now to cur)
                        }
                        val bucketMs = when (hafRange) {
                            72L -> 4 * 3600_000L
                            168L -> 8 * 3600_000L
                            else -> 0L
                        }
                        val chartPoints = if (bucketMs <= 0) {
                            val out = ArrayList<Pair<Long, Double>>()
                            points.forEach { p ->
                                if (out.isEmpty() || out.last().second != p.second) out.add(p)
                            }
                            out
                        } else points
                            .groupBy { it.first / bucketMs }
                            .map { (bucket, group) ->
                                val peak = group.maxBy { it.second }
                                (bucket * bucketMs) to peak.second
                            }
                            .sortedBy { it.first }
                        if (chartPoints.size < 2) {
                            Text(
                                "正在记录，至少需要两个时间点才能画图",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        } else {
                            val min = chartPoints.minOf { it.second }
                            val max = chartPoints.maxOf { it.second }
                            val lineColor = Line
                            val mintColor = Mint
                            Box(Modifier.fillMaxWidth().height(150.dp)) {
                                Canvas(
                                    Modifier.fillMaxSize().onGloballyPositioned { hafChartWidth = it.size.width.toFloat() }.pointerInput(chartPoints) {
                                        detectTapGestures(onPress = { off ->
                                            val idx = ((off.x / size.width) * (chartPoints.size - 1))
                                                .roundToInt().coerceIn(0, chartPoints.size - 1)
                                            hafHoverIndex = idx
                                            hafHoverX = size.width * idx / (chartPoints.size - 1).toFloat()
                                        })
                                    }
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val top = 8.dp.toPx()
                                    val bottom = h - 8.dp.toPx()
                                    val range = (max - min).coerceAtLeast(1.0)
                                    for (i in 0..4) {
                                        val y = top + (bottom - top) * i / 4f
                                        drawLine(lineColor.copy(alpha = 0.35f), Offset(0f, y), Offset(w, y), 1f)
                                    }
                                    val path = Path()
                                    chartPoints.forEachIndexed { i, (_, v) ->
                                        val x = if (chartPoints.size == 1) 0f else w * i / (chartPoints.size - 1).toFloat()
                                        val y = bottom - ((v - min) / range * (bottom - top)).toFloat()
                                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }
                                    drawPath(path, mintColor, style = Stroke(width = 2.dp.toPx()))
                                    chartPoints.forEachIndexed { i, (_, v) ->
                                        val x = if (chartPoints.size == 1) 0f else w * i / (chartPoints.size - 1).toFloat()
                                        val y = bottom - ((v - min) / range * (bottom - top)).toFloat()
                                        drawCircle(mintColor.copy(alpha = 0.75f), 2.5.dp.toPx(), Offset(x, y))
                                    }
                                    hafHoverIndex?.let { idx ->
                                        val hp = chartPoints.getOrNull(idx) ?: return@let
                                        val x = w * idx / (chartPoints.size - 1).toFloat()
                                        val y = bottom - ((hp.second - min) / range * (bottom - top)).toFloat()
                                        drawLine(mintColor.copy(alpha = 0.4f), Offset(x, top), Offset(x, bottom), 1f)
                                        drawCircle(mintColor, 4.dp.toPx(), Offset(x, y))
                                    }
                                }
                                hafHoverIndex?.let { idx ->
                                    val hp = chartPoints.getOrNull(idx) ?: return@let
                                    Surface(
                                        modifier = Modifier
                                            .align(if (hafHoverX > hafChartWidth / 2) Alignment.TopEnd else Alignment.TopStart)
                                            .padding(2.dp),
                                        color = Surface2,
                                        shape = RoundedCornerShape(6.dp),
                                        shadowElevation = 4.dp
                                    ) {
                                        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text(
                                                formatWanAmount(hp.second),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(hp.first),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "最低 ${formatWanAmount(min)} · 最高 ${formatWanAmount(max)} · 当前 ${formatWanAmount(chartPoints.last().second)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            if (detailSection == 0) {
                item(key = "stamina") {
                    DashaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("体力 / 负重 / AW", modifier = Modifier.weight(1f))
                            if (levels != null) {
                                Text(
                                    "AI 识别 · ${formatTimestamp(levels.updatedAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row {
                            StatBlock(
                                value = levels?.training?.let { "Lv$it" } ?: "—",
                                label = "体力（训练中心）",
                                modifier = Modifier.weight(1f),
                                valueColor = Mint
                            )
                            StatBlock(
                                value = levels?.range?.let { "Lv$it" } ?: "—",
                                label = "负重（靶场）",
                                modifier = Modifier.weight(1f),
                                valueColor = Sky
                            )
                            StatBlock(
                                value = levels?.awBullets?.let { "${it}发" } ?: "—",
                                label = "AW 子弹",
                                modifier = Modifier.weight(1f),
                                valueColor = Amber
                            )
                        }
                        if (levels == null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "未登记：可在「连接设置 → 体力 / 负重 / AW（AI 识别）」里识别所有账号",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            if (detailSection == 2) {
                item(key = "actions") {
                    com.dashabi.control.ui.components.AccountOperationsPanel(
                        paused = account.paused, busyAction = vm.busyAction,
                        onTogglePause = { if (account.paused) vm.resume() else vm.pause() },
                        onProduction = { vm.runProduction() },
                        onSell = { showSellDialog = true },
                        onSchedule = { showScheduleDialog = true }
                    )
                }
            }

            if (detailSection == 2) {
                item(key = "qrlogin-card") {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val cs = androidx.compose.runtime.remember {
                        com.dashabi.control.data.CloudSync(ctx, container.stamina, container.credit, container.gameNames)
                    }
                    var busy by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    var canceled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    var msg by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf<String?>(null)
                    }
                    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            busy = true
                            scope.launch {
                                val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    runCatching {
                                        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    }.getOrNull()
                                }
                                val url = bytes?.let { cs.uploadQr(it) }
                                // 先看设备原状态：运行中才停脚本，定时开着才关，结束要按原样恢复
                                // /devices/{code} 的 timer_enabled 经常是 null，必须读 /status 实时状态
                                val devBefore = (container.repo.deviceLive(deviceCode) as? com.dashabi.control.data.ApiResult.Ok)?.value?.status
                                val wasRunning = devBefore?.running == true
                                val wasTimer = devBefore?.timerEnabled == true
                                if (wasRunning) runCatching {
                                    container.repo.stop(deviceCode, disableTimer = false)
                                }
                                if (wasTimer) runCatching { container.repo.timerDisable(deviceCode) }
                                val target = com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode)
                                val sentId = if (url != null && !target.isNullOrBlank()) {
                                    cs.sendQrLogin(target, accountCode, url)
                                } else null
                                val sent = sentId != null
                                msg = when {
                                    bytes == null -> "读取图片失败"
                                    url == null -> "上传失败"
                                    target.isNullOrBlank() -> "这台设备还没绑定云控助手"
                                    sent -> "扫码指令已下发，等待结果…"
                                    else -> "下发失败"
                                }
                                // 轮询云控助手回报的结果（最多 90 秒）
                                if (sent && !target.isNullOrBlank() && sentId != null) {
                                    val started = System.currentTimeMillis()
                                    val since = 0L
                                    while (System.currentTimeMillis() - started < 120_000 && !canceled) {
                                        kotlinx.coroutines.delay(2500)
                                        val st = cs.qrStatus(target, sentId, since)
                                        if (st != null) {
                                            msg = st
                                            if (st.startsWith("✅")) {
                                                // 登录成功后自动暂停当前账号，避免马上继续跑任务
                                                msg = st + "，已自动暂停该账号"
                                                vm.pause()
                                            }
                                            if (st.startsWith("✅") || st.startsWith("❌")) break
                                        }
                                    }
                                }
                                // 恢复到开始前的状态
                                if (wasTimer) runCatching { container.repo.timerEnable(deviceCode) }
                                // 界面关掉也不影响：110 秒后再兜底开一次
                                if (wasTimer) {
                                    com.dashabi.control.DashaApp.timerRestoreScope.launch {
                                        kotlinx.coroutines.delay(110_000)
                                        runCatching { container.repo.timerEnable(deviceCode) }
                                    }
                                }
                                busy = false
                            }
                        }
                    }
                    DashaCard(accent = Sky) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                "扫码登录",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "上传 QQ 登录二维码，云控助手会切到该账号并扫码登录",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "确保图片里只有一个二维码！！！",
                                style = MaterialTheme.typography.labelMedium,
                                color = Rose
                            )
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedButton(
                                enabled = !busy,
                                onClick = { picker.launch("image/*") },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(if (busy) "处理中…" else "上传二维码并扫码") }
                            if (busy) {
                                Spacer(Modifier.width(8.dp))
                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        canceled = true
                                        msg = "已停止"
                                        com.dashabi.control.remote.RebootBindStore.get(ctx, deviceCode)?.let { assistant ->
                                            scope.launch { cs.cancelQr(assistant) }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("停止") }
                            }
                            msg?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(it, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                    StatsQueryCard(container = container, account = account, deviceCode = deviceCode)

                }
            }

            if (detailSection == 1) {
                item(key = "facilities") {
                    DashaCard {
                        SectionTitle("设施进度")
                        Spacer(Modifier.height(8.dp))
                        FacilityRows(account.facilities)
                    }
                }
            }

            if (detailSection == 3) {
                item(key = "images") {
                    DashaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("账号截图", modifier = Modifier.weight(1f))
                            Text(
                                "images",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PreviewTile(
                                title = "个人信息",
                                info = vm.images?.profile,
                                url = previewUrl("profile"),
                                modifier = Modifier.weight(1f),
                                onOpen = {
                                    previewUrl("profile")?.let {
                                        preview = PreviewImage("个人信息", it, vm.images?.profile?.capturedAt)
                                    }
                                }
                            )
                            PreviewTile(
                                title = "特勤处",
                                info = vm.images?.tqc,
                                url = previewUrl("tqc"),
                                modifier = Modifier.weight(1f),
                                onOpen = {
                                    previewUrl("tqc")?.let {
                                        preview = PreviewImage("特勤处", it, vm.images?.tqc?.capturedAt)
                                    }
                                }
                            )
                            PreviewTile(
                                title = "仓库",
                                info = vm.images?.store,
                                url = previewUrl("store"),
                                modifier = Modifier.weight(1f),
                                onOpen = {
                                    previewUrl("store")?.let {
                                        preview = PreviewImage("仓库", it, vm.images?.store?.capturedAt)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (detailSection == 3) {
                item(key = "annotation") {
                    DashaCard {
                        Text("账号标注", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = annotationText, onValueChange = { annotationText = it.take(500) },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5,
                            placeholder = { Text("记录账号用途、跟进事项或备注") }, shape = RoundedCornerShape(12.dp))
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = !annotationSaving, modifier = Modifier.weight(1f), onClick = {
                                annotationSaving = true
                                scope.launch {
                                    val ok = runCatching { container.cloudSync.deleteAnnotation(accountCode) }.getOrDefault(false)
                                    if (ok) annotationText = ""
                                    annotationSaving = false
                                    snackbar.showSnackbar(if (ok) "标注已删除" else "删除失败")
                                }
                            }) { Text("删除标注", color = Rose) }
                            Button(enabled = !annotationSaving, modifier = Modifier.weight(1f), onClick = {
                                annotationSaving = true
                                scope.launch {
                                    val ok = runCatching { container.cloudSync.setAnnotation(accountCode, annotationText.trim()) }.getOrDefault(false)
                                    annotationSaving = false
                                    snackbar.showSnackbar(if (ok) "标注已保存" else "保存失败")
                                }
                            }) { Text(if (annotationSaving) "处理中…" else "保存标注") }
                        }
                    }
                }
            }

            if (detailSection == 3) {
                item(key = "meta") {
                    DashaCard {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { metaExpanded = !metaExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionTitle("接口字段", modifier = Modifier.weight(1f))
                            Text(
                                if (metaExpanded) "收起" else "展开",
                                style = MaterialTheme.typography.labelMedium,
                                color = Amber
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = if (metaExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = if (metaExpanded) "收起接口字段" else "展开接口字段",
                                tint = Amber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        AnimatedVisibility(visible = metaExpanded) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                KeyValueRow("account_code", account.accountCode, mono = true)
                                KeyValueRow("device_code", account.deviceCode, mono = true)
                                KeyValueRow("game_id", account.gameId, mono = true)
                                KeyValueRow("game_nick", account.gameNick)
                                KeyValueRow("status", account.status, mono = true)
                                KeyValueRow("raw_status", account.rawStatus)
                                KeyValueRow("paused", account.paused.toString(), mono = true)
                                KeyValueRow("login_type", account.loginType, mono = true)
                            }
                        }
                    }
                }
            }

            if (detailSection == 3) {
                item(key = "notes") {
                    DashaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("备注", modifier = Modifier.weight(1f))
                            TextButton(onClick = { showNoteDialog = true }) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = Amber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("编辑", color = Amber, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        KeyValueRow("备注", account.upstreamRemark, valueColor = TextSecondary)
                        KeyValueRow("网站备注", account.websiteNote, valueColor = Amber)
                    }
                }
            }

            if (detailSection == 2) {
                item(key = "schedules") {
                    DashaCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("定时暂停", modifier = Modifier.weight(1f))
                            TextButton(onClick = { showScheduleDialog = true }) {
                                Text("+ 新建", color = Amber, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        if (vm.schedules.isEmpty()) {
                            Text(
                                "当前账号没有定时暂停计划",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        } else {
                            vm.schedules.forEach { schedule ->
                                ScheduleRow(
                                    schedule = schedule,
                                    onCancel = {
                                        val id = (schedule["id"] as? JsonPrimitive)?.longOrNull
                                        if (id != null) pendingCancelId = id
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

    }

    preview?.let { item ->
        ImagePreviewDialog(item) { preview = null }
    }

    if (showNoteDialog) {
        NoteDialog(
            initial = vm.account?.websiteNote.orEmpty(),
            onDismiss = { showNoteDialog = false },
            onSave = { note ->
                vm.saveNote(note)
                showNoteDialog = false
            }
        )
    }

    if (showScheduleDialog) {
        ScheduleDialog(
            onDismiss = { showScheduleDialog = false },
            onCreate = { start, end ->
                vm.createSchedule(start, end)
                showScheduleDialog = false
            }
        )
    }

    if (showSellDialog) {
        ConfirmDialog(
            title = "出售这个账号？",
            message = "将提交「${vm.account?.gameNick ?: accountCode}」的出售任务，出售后不可撤销。\n" +
                "仅当设备处于「等待下一轮」时才会受理。",
            confirmText = "确认出售",
            destructive = true,
            onConfirm = {
                vm.sell()
                showSellDialog = false
            },
            onDismiss = { showSellDialog = false }
        )
    }

    pendingCancelId?.let { id ->
        ConfirmDialog(
            title = "取消定时暂停？",
            message = "将删除该定时暂停计划（id=$id），已暂停的账号不会自动恢复。",
            confirmText = "取消计划",
            destructive = true,
            onConfirm = {
                vm.cancelSchedule(id)
                pendingCancelId = null
            },
            onDismiss = { pendingCancelId = null }
        )
    }
}

/* ------------------------------------------------------------------ */
/*  子组件                                                              */
/* ------------------------------------------------------------------ */

private val facilityLabels = listOf(
    "tech" to "技术中心",
    "workbench" to "工作台",
    "pharmacy" to "制药台",
    "armory" to "防具台"
)

@Composable
private fun FacilityRows(facilities: Facilities?) {
    facilityLabels.forEachIndexed { index, (key, label) ->
        val facility: Facility? = when (key) {
            "tech" -> facilities?.tech
            "workbench" -> facilities?.workbench
            "pharmacy" -> facilities?.pharmacy
            else -> facilities?.armory
        }
        val level = facility?.level
        val activeColor = when {
            level == null -> Surface2
            level >= 4 -> Mint
            level == 3 -> Mint.copy(alpha = 0.8f)
            level == 2 -> Sky
            else -> Amber
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.width(64.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(4) { i ->
                    val active = level != null && i < level
                    Box(
                        Modifier
                            .width(14.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (active) activeColor else Surface2)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                level?.let { "Lv$it" } ?: "—",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))
            Text(
                facility?.finishAt?.takeIf { it.isNotBlank() } ?: "—",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = TextMuted
            )
        }
        if (index < facilityLabels.lastIndex) {
            HorizontalDivider(color = Line.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun PreviewTile(
    title: String,
    info: ImageInfo?,
    url: String?,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface2)
                .clickable(enabled = url != null, onClick = onOpen),
            contentAlignment = Alignment.Center
        ) {
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(
            info?.capturedAt?.let { com.dashabi.control.ui.formatIso(it) } ?: "暂无",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun ScheduleRow(schedule: JsonObject, onCancel: () -> Unit) {
    val start = jsonString(schedule, "start_at") ?: jsonString(schedule, "start")
    val end = jsonString(schedule, "end_at") ?: jsonString(schedule, "end")
    val status = jsonString(schedule, "status") ?: jsonString(schedule, "state")
    val id = (schedule["id"] as? JsonPrimitive)?.longOrNull

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (id != null) {
                    Text(
                        "#$id",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Amber
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    if (!status.isNullOrBlank()) status else "定时暂停",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                "${start ?: "—"}  →  ${end ?: "—"}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = TextPrimary,
                maxLines = 2
            )
        }
        IconButton(onClick = onCancel, enabled = id != null) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "取消计划",
                tint = if (id != null) Rose else TextMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/*  弹窗                                                                */
/* ------------------------------------------------------------------ */

@Composable
private fun ImagePreviewDialog(image: PreviewImage, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = image.url,
                contentDescription = image.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(vertical = 56.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(image.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(
                        image.capturedAt ?: "—",
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = Color.White)
                }
            }
        }
    }
}

@Composable
internal fun NoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("修改网站备注", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "备注只写入网站本地，不会同步到云控软件。最多 200 字。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 200) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入备注…", color = TextMuted) },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber.copy(alpha = 0.6f),
                        unfocusedBorderColor = Line,
                        focusedContainerColor = Surface1,
                        unfocusedContainerColor = Surface1,
                        cursorColor = Amber,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${text.length}/200",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.trim()) }) {
                Text("保存", color = Amber)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun ScheduleDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") }
    val now = remember { LocalDateTime.now() }
    var start by remember {
        mutableStateOf(now.plusDays(1).withHour(1).withMinute(0).withSecond(0).format(formatter))
    }
    var end by remember {
        mutableStateOf(now.plusDays(1).withHour(6).withMinute(0).withSecond(0).format(formatter))
    }
    var error by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    fun chooseTime(value: String, onValue: (String) -> Unit) {
        val selected = runCatching { LocalDateTime.parse(value, formatter) }.getOrDefault(LocalDateTime.now())
        android.app.DatePickerDialog(context, { _, year, month, day ->
            android.app.TimePickerDialog(context, { _, hour, minute ->
                onValue(LocalDateTime.of(year, month + 1, day, hour, minute, 0).format(formatter))
                error = null
            }, selected.hour, selected.minute, true).show()
        }, selected.year, selected.monthValue - 1, selected.dayOfMonth).show()
    }

    fun validate(): Boolean {
        val s = runCatching { LocalDateTime.parse(start, formatter) }.getOrNull()
        val e = runCatching { LocalDateTime.parse(end, formatter) }.getOrNull()
        if (s == null || e == null) {
            error = "时间格式应为 yyyy-MM-dd HH:mm:ss"
            return false
        }
        if (!e.isAfter(s)) {
            error = "结束时间必须晚于开始时间"
            return false
        }
        error = null
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("新建定时暂停", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "在指定时间区间内自动暂停账号，到点后由云控恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it; error = null },
                    trailingIcon = { IconButton(onClick = { chooseTime(start) { start = it } }) { Icon(Icons.Outlined.Schedule, "选择开始时间") } },
                    label = { Text("开始时间") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber.copy(alpha = 0.6f),
                        unfocusedBorderColor = Line,
                        focusedContainerColor = Surface1,
                        unfocusedContainerColor = Surface1,
                        cursorColor = Amber,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it; error = null },
                    trailingIcon = { IconButton(onClick = { chooseTime(end) { end = it } }) { Icon(Icons.Outlined.Schedule, "选择结束时间") } },
                    label = { Text("结束时间") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Amber.copy(alpha = 0.6f),
                        unfocusedBorderColor = Line,
                        focusedContainerColor = Surface1,
                        unfocusedContainerColor = Surface1,
                        cursorColor = Amber,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = Rose, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (validate()) onCreate(start, end) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = OnAccent
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}
