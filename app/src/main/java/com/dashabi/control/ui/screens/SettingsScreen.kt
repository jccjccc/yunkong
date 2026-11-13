package com.dashabi.control.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dashabi.control.AppContainer
import com.dashabi.control.BuildConfig
import com.dashabi.control.data.ApiResult
import com.dashabi.control.data.CloudSync
import com.dashabi.control.data.License
import com.dashabi.control.data.Me
import com.dashabi.control.data.ThemeMode
import com.dashabi.control.downloadAndInstall
import com.dashabi.control.ui.components.*
import com.dashabi.control.ui.scopeLabel
import com.dashabi.control.ui.theme.*
import com.dashabi.control.ui.vm.RecognizeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject

private data class AvailableUpdate(val version: String, val url: String, val sha: String, val notes: String)

private data class TestResult(val ok: Boolean, val message: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onStartRecognition: (Boolean, RecognizeMode) -> Unit,
    onSelectTab: ((Int) -> Unit)? = null,
    abnormalCount: Int = 0,
    deviceAbnormalCount: Int = 0
) {
    var destination by rememberSaveable { mutableStateOf(SettingsDestination.OVERVIEW) }
    val scope = rememberCoroutineScope()
    val initial = container.settings.current()

    var key by remember { mutableStateOf(initial.apiKey) }
    var base by remember { mutableStateOf(initial.baseUrl) }
    var control by remember { mutableStateOf(container.settings.controlToken()) }
    var reveal by remember { mutableStateOf(false) }
    var revealControl by remember { mutableStateOf(false) }
    val baseValid = serviceAddressValid(base)
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TestResult?>(null) }
    var me by remember { mutableStateOf<Me?>(null) }

    val aiInitial = container.aiSettings.current()
    var aiKey by remember { mutableStateOf(aiInitial.apiKey) }
    var aiBase by remember { mutableStateOf(aiInitial.baseUrl) }
    var aiModel by remember { mutableStateOf(aiInitial.model) }
    var onlyMissing by remember { mutableStateOf(true) }
    val staminaLevels by container.stamina.state.collectAsState()
    val context = LocalContext.current
    val cloudSync = remember { CloudSync(context, container.stamina, container.credit, container.gameNames) }
    val license = remember { License(context) }
    var whitelisted by remember { mutableStateOf(false) }
    var deviceList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var deviceListLoading by remember { mutableStateOf(false) }
    var deviceListError by remember { mutableStateOf<String?>(null) }
    var deviceSortExpanded by remember { mutableStateOf(true) }
    val deviceOrder by container.deviceOrder.order.collectAsState()
    suspend fun loadDeviceOrder() {
        if (deviceListLoading || container.settings.current().apiKey.isBlank()) return
        deviceListLoading = true
        deviceListError = null
        try {
            when (val response = withContext(Dispatchers.IO) { container.repo.accounts() }) {
                is ApiResult.Ok -> deviceList = response.value.mapNotNull { account ->
                    val code = account.deviceCode ?: return@mapNotNull null
                    code to (account.deviceName ?: code)
                }.distinctBy { it.first }
                is ApiResult.Err -> deviceListError = response.message
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            deviceListError = "读取设备失败，请检查连接后重试"
        } finally {
            deviceListLoading = false
        }
    }
    LaunchedEffect(destination) {
        if (destination == SettingsDestination.SYNC) whitelisted = withContext(Dispatchers.IO) {
            runCatching { license.whitelisted() }.getOrDefault(false)
        }
        if (destination == SettingsDestination.DEVICE_ORDER) loadDeviceOrder()
    }
    val clipboard = LocalClipboardManager.current
    var peerCode by remember { mutableStateOf(cloudSync.peerCode) }
    var cloudStatus by remember { mutableStateOf<String?>(null) }
    var cloudBusy by remember { mutableStateOf(false) }
    var updateBusy by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    val updateScope = rememberCoroutineScope()
    var availableUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }
    fun checkUpdate() {
        if (updateBusy) return
        updateBusy = true
        updateStatus = "正在检查可用版本…"
        updateScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val request = okhttp3.Request.Builder()
                        .url("https://dasha-update-1313972671.cos.ap-shanghai.myqcloud.com/latest-android.json")
                        .header("User-Agent", "DashaControl/1.0 (Android)").build()
                    okhttp3.OkHttpClient().newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("HTTP ${response.code}")
                        val obj = kotlinx.serialization.json.Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                        fun field(name: String) = (obj[name] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                        val version = field("version")
                        require(version.isNotBlank()) { "版本信息为空" }
                        AvailableUpdate(version, field("url"), field("sha256"), field("notes"))
                    }
                }
            }
            result.onSuccess { update ->
                availableUpdate = update.takeIf { compareVer(it.version, BuildConfig.VERSION_NAME.substringBefore("-")) > 0 }
                updateStatus = if (availableUpdate == null) "当前已是最新版本" else "发现新版本，确认后下载并安装"
            }.onFailure { updateStatus = "检查失败：${it.message}，可点击重试" }
            updateBusy = false
        }
    }
    fun installUpdate() {
        val update = availableUpdate ?: return
        if (updateBusy) return
        updateBusy = true
        updateStatus = "正在下载并校验安装包…"
        downloadAndInstall(context, update.url, update.sha,
            onStatus = { updateStatus = it }, onFinished = { updateBusy = false })
    }
    val themeMode by container.theme.mode.collectAsState()

    fun saveAi() {
        container.aiSettings.save(aiKey, aiBase, aiModel)
    }

    LaunchedEffect(Unit) {
        if (container.settings.current().apiKey.isBlank()) return@LaunchedEffect
        when (val r = container.repo.me()) {
            is ApiResult.Ok -> {
                me = r.value
            }
            is ApiResult.Err -> {
                result = TestResult(false, r.message)
            }
        }
    }

    fun runTest(save: Boolean) {
        if (testing || !serviceAddressValid(if (save) base else container.settings.current().baseUrl)) return
        if (save) {
            container.settings.save(key, base)
            container.settings.saveControlToken(control)
            onSaved()
        }
        testing = true
        result = null
        scope.launch {
            when (val r = container.repo.me()) {
                is ApiResult.Ok -> {
                    me = r.value
                    key = container.settings.current().apiKey
                    base = container.settings.current().baseUrl
                    result = TestResult(true, "连接成功，身份：${r.value.name ?: "未知"}，权限 ${r.value.scopes.size} 项")
                }
                is ApiResult.Err -> result = TestResult(false, r.message)
            }
            testing = false
        }
    }

    BackHandler(enabled = destination != SettingsDestination.OVERVIEW) { destination = SettingsDestination.OVERVIEW }
    Scaffold(
        containerColor = Ink,
        topBar = {
            DashaTopBar(
                title = destination.title,
                subtitle = if (destination == SettingsDestination.OVERVIEW) "服务连接与工作偏好" else "设置 / ${destination.title}",
                onBack = if (destination != SettingsDestination.OVERVIEW) ({ destination = SettingsDestination.OVERVIEW })
                    else if (onSelectTab == null) onDismiss else null
            )
        },
        bottomBar = {
            Column {
                if (destination == SettingsDestination.CONNECTION || destination == SettingsDestination.INTELLIGENCE) {
                    Surface(color = Surface1, shadowElevation = 0.dp) {
                        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (destination == SettingsDestination.CONNECTION) {
                                Button(onClick = { runTest(true) }, enabled = !testing && key.isNotBlank() && baseValid, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                    if (testing) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OnAccent); Spacer(Modifier.width(8.dp)) }
                                    Text(if (testing) "正在测试连接…" else "保存并测试连接")
                                }
                            }
                            OutlinedButton(onClick = {
                                if (destination == SettingsDestination.CONNECTION) {
                                    container.settings.save(key, base)
                                    container.settings.saveControlToken(control)
                                } else saveAi()
                                onSaved()
                                destination = SettingsDestination.OVERVIEW
                            }, enabled = !testing && (destination != SettingsDestination.CONNECTION || baseValid), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保存设置") }
                        }
                    }
                }
                onSelectTab?.let { navigate ->
                    DashaBottomBar(3, Icons.Outlined.Devices, Icons.Outlined.Person, Icons.Outlined.WarningAmber,
                        abnormalCount, deviceAbnormalCount, navigate)
                }
            }
        }
    ) { insets ->
        androidx.compose.runtime.key(destination) {
            Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .imePadding().padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (destination != SettingsDestination.OVERVIEW) {
                    PageSectionIntro(destination.title, destination.description, destination.icon)
                }
                when (destination) {
                    SettingsDestination.OVERVIEW -> SettingsHub(
                        identity = me?.name?.takeIf { it.isNotBlank() } ?: if (key.isBlank()) "连接你的工作空间" else "已配置服务连接",
                        connection = if (me != null) "身份已验证 · ${me?.scopes?.size ?: 0} 项可用权限" else if (key.isBlank()) "配置 API Key 后开始管理设备与账号" else "进入服务连接，测试身份与访问权限",
                        version = BuildConfig.VERSION_NAME,
                        onOpen = { destination = it }
                    )
                    SettingsDestination.CONNECTION -> {
                        Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Surface2)
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Outlined.Lock,
                                                    contentDescription = null,
                                                    tint = Amber,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "当前身份",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Amber
                                                )
                                                Spacer(Modifier.weight(1f))
                                                if (testing) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        color = Amber,
                                                        strokeWidth = 2.dp
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                me?.name?.takeIf { it.isNotBlank() }
                                                    ?: if (initial.apiKey.isBlank()) "未配置 Key" else "未验证",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                me?.scopes?.forEach { scopeName ->
                                                    InfoChip(scopeLabel(scopeName), Mint)
                                                }
                                                if (me == null) {
                                                    InfoChip("尚未获取权限", TextMuted)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))
                                    SectionTitle("开放 API Key")
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = key,
                                        enabled = !testing,
                                        onValueChange = { key = it; me = null; result = null },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("zh_live_…", color = TextMuted) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            TextButton(onClick = { reveal = !reveal }, modifier = Modifier.testTag("reveal_api_key")) {
                                                Text(
                                                    if (reveal) "隐藏" else "显示",
                                                    color = Amber,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Amber.copy(alpha = 0.55f),
                                            unfocusedBorderColor = Line,
                                            focusedContainerColor = Surface2,
                                            unfocusedContainerColor = Surface2,
                                            cursorColor = Amber,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(Modifier.height(14.dp))
                                    SectionTitle("服务地址")
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = base,
                                        enabled = !testing,
                                        isError = !baseValid,
                                        supportingText = { Text(if (baseValid) "完整 API 地址，支持 HTTP 或 HTTPS。" else "请输入有效的 HTTP 或 HTTPS 服务地址", color = if (baseValid) TextMuted else Rose) },
                                        onValueChange = { base = it; me = null; result = null },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("https://a.dasha.bi/index.php/open/v1", color = TextMuted) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Amber.copy(alpha = 0.55f),
                                            unfocusedBorderColor = Line,
                                            focusedContainerColor = Surface2,
                                            unfocusedContainerColor = Surface2,
                                            cursorColor = Amber,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(Modifier.height(14.dp))
                                    SectionTitle("安全令牌")
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = control,
                                        enabled = !testing,
                                        trailingIcon = {
                                            TextButton(onClick = { revealControl = !revealControl }, modifier = Modifier.testTag("reveal_control_token")) {
                                                Text(if (revealControl) "隐藏" else "显示")
                                            }
                                        },
                                        visualTransformation = if (revealControl) VisualTransformation.None else PasswordVisualTransformation(),
                                        onValueChange = { control = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("从云端复制，不再内置", color = TextMuted) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Amber.copy(alpha = 0.55f),
                                            unfocusedBorderColor = Line,
                                            focusedContainerColor = Surface2,
                                            unfocusedContainerColor = Surface2,
                                            cursorColor = Amber,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                        Spacer(Modifier.height(4.dp))
                                    Text(
                                        "默认：${BuildConfig.DEFAULT_BASE_URL}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    result?.let { r ->
                                        Spacer(Modifier.height(12.dp))
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background((if (r.ok) Mint else Rose).copy(alpha = 0.10f))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (r.ok) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                                                contentDescription = null,
                                                tint = if (r.ok) Mint else Rose,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(7.dp))
                                            Text(
                                                r.message,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (r.ok) Mint else Rose
                                            )
                                        }
                                    }
                        TextButton(
                            onClick = {
                                container.settings.resetToDefault()
                                key = container.settings.current().apiKey
                                base = container.settings.current().baseUrl
                                runTest(false)
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
                        ) {
                            Text("恢复默认 Key 与地址", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    SettingsDestination.APPEARANCE -> {
                        DashaCard {
                            Text("显示模式", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text("主题即时生效，所有页面保持一致。", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(Modifier.height(16.dp))
                            ThemeMode.entries.forEach { mode ->
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(mode.label) },
                                    trailingContent = { RadioButton(selected = themeMode == mode, onClick = null) },
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).selectable(selected = themeMode == mode,
                                        role = Role.RadioButton, onClick = { container.theme.set(mode) }),
                                    colors = ListItemDefaults.colors(containerColor = Surface1)
                                )
                            }
                        }
                    }
                    SettingsDestination.SYNC -> {
                        DashaCard {
                            Text("设备互通", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text("同步体力、负重、AW、信用分与助手绑定信息。", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("本机设备码", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                    Text(cloudSync.deviceCode, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = TextPrimary)
                                }
                                TextButton(onClick = { clipboard.setText(AnnotatedString(cloudSync.deviceCode)); cloudStatus = "设备码已复制" }) { Text("复制") }
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = peerCode, onValueChange = { peerCode = it; cloudSync.peerCode = it },
                                label = { Text("对方设备码（电脑）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {
                                if (!cloudBusy) {
                                    cloudBusy = true; cloudStatus = "正在同步…"
                                    updateScope.launch {
                                        cloudStatus = withContext(Dispatchers.IO) { runCatching { cloudSync.sync() }.getOrElse { "同步失败：${it.message}" } }
                                        cloudBusy = false
                                    }
                                }
                            }, enabled = !cloudBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (cloudBusy) "同步中…" else "立即同步数据") }
                            com.dashabi.control.ui.vm.SyncStatus.last?.let { Text("上次同步：$it", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
                        }
                        DashaCard {
                            Text("使用授权", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(if (whitelisted) "当前设备已授权" else "可刷新设备授权状态", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            val trialEnd = license.trialStart() + 24 * 60 * 60 * 1000L
                            if (!whitelisted && System.currentTimeMillis() < trialEnd) {
                                Text("试用到期 ${com.dashabi.control.ui.formatTimestamp(trialEnd)}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = {
                                updateScope.launch {
                                    val ok = withContext(Dispatchers.IO) { runCatching { license.whitelisted() }.getOrDefault(false) }
                                    whitelisted = ok
                                    cloudStatus = if (ok) "已授权，重启 App 生效" else "未授权或已失效"
                                }
                            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("刷新授权状态") }
                        }
                        cloudStatus?.let { OperationFeedback(it, it.contains("失败") || it.contains("失效")) }
                    }
                    SettingsDestination.DEVICE_ORDER -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("设备排序", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("调整账号总览设备筛选顺序", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            TextButton(onClick = { deviceSortExpanded = !deviceSortExpanded }) {
                                Text(if (deviceSortExpanded) "收起 ▴" else "展开 ▾", color = TextSecondary)
                            }
                        }
                        if (deviceSortExpanded) {
                            val orderedDevices = deviceList.sortedWith { x, y ->
                                val xi = deviceOrder.indexOf(x.first)
                                val yi = deviceOrder.indexOf(y.first)
                                when {
                                    xi >= 0 && yi >= 0 -> xi.compareTo(yi)
                                    xi >= 0 -> -1
                                    yi >= 0 -> 1
                                    else -> x.second.compareTo(y.second)
                                }
                            }
                            orderedDevices.forEach { (code, name) ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, Modifier.weight(1f), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = {
                                        container.deviceOrder.move(code, -1, orderedDevices.map { it.first })
                                    }) { Text("↑", color = TextSecondary) }
                                    TextButton(onClick = {
                                        container.deviceOrder.move(code, 1, orderedDevices.map { it.first })
                                    }) { Text("↓", color = TextSecondary) }
                                }
                            }
                        }
                        if (deviceListLoading) LoadingState("正在读取设备列表…")
                        deviceListError?.let { message -> ErrorState(message, onRetry = { scope.launch { loadDeviceOrder() } }) }
                        if (deviceList.isEmpty() && !deviceListLoading && deviceListError == null) {
                            EmptyState("暂无可排序的设备", "配置服务连接并同步账号后，可调整设备显示顺序。")
                        }
                    }
                    SettingsDestination.INTELLIGENCE -> {
                        SectionTitle("体力 / 负重 / AW（AI 识别）")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "用 DeepSeek 视觉模型读取每个账号「特勤处」截图中的训练中心（体力）、靶场（负重）等级，以及「仓库」截图中 .338AP（AW）子弹的数量，识别结果保存在本机。",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = aiKey,
                            onValueChange = { aiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("DeepSeek API Key（sk-…）", color = TextMuted) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Amber.copy(alpha = 0.55f),
                                unfocusedBorderColor = Line,
                                focusedContainerColor = Surface2,
                                unfocusedContainerColor = Surface2,
                                cursorColor = Amber,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = aiBase,
                                onValueChange = { aiBase = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("API 地址", style = MaterialTheme.typography.labelSmall) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Amber.copy(alpha = 0.55f),
                                    unfocusedBorderColor = Line,
                                    focusedContainerColor = Surface2,
                                    unfocusedContainerColor = Surface2,
                                    cursorColor = Amber,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = aiModel,
                                onValueChange = { aiModel = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("模型", style = MaterialTheme.typography.labelSmall) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Amber.copy(alpha = 0.55f),
                                    unfocusedBorderColor = Line,
                                    focusedContainerColor = Surface2,
                                    unfocusedContainerColor = Surface2,
                                    cursorColor = Amber,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = onlyMissing,
                                onCheckedChange = { onlyMissing = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Amber,
                                    uncheckedColor = TextMuted
                                )
                            )
                            Text(
                                "只识别未登记的账号",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "已登记 ${staminaLevels.size} 个",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = {
                                saveAi()
                                onStartRecognition(onlyMissing, RecognizeMode.LEVELS)
                            },
                            enabled = aiKey.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Amber,
                                contentColor = OnAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("识别体力 / 负重", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                saveAi()
                                onStartRecognition(onlyMissing, RecognizeMode.AW)
                            },
                            enabled = aiKey.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Amber.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("识别 AW 子弹", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "两个按钮分开识别：体力/负重读特勤处截图，AW 读仓库截图；每张截图约 1000 tokens，可随时取消。",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    SettingsDestination.UPDATE -> {
                        UpdatePanel(BuildConfig.VERSION_NAME, availableUpdate?.version, availableUpdate?.notes.orEmpty(),
                            updateBusy, updateStatus, { checkUpdate() }, { installUpdate() })
                        OperationFeedback("此处更新手机端客户端。云控助手请前往设备详情中的「助手管理」更新。")
                    }
                    SettingsDestination.HELP -> {
                        ManualSection("1. 首次使用", "设置里填 API Key → 填安全令牌 → 绑定云控助手设备码。绑定后设备详情才能远程重启、更新、亮度、扫码。")
                        ManualSection("2. 账号总览", "可按设备、状态、信用分筛选；点排序切换哈夫币/体力/AW/等级/资产。卡片上的「24h 峰值对比」是当前值对比昨日 6:00-今日 6:00 峰值，持平显示「持平」，没有数据显示「—」。")
                        ManualSection("3. 标注文字", "账号详情 → 资料 → 账号标注，保存后服务器同步；总览会显示在备注后面，也能被搜索。删除会同时删除服务器记录。")
                        ManualSection("4. 哈夫币变化图", "24小时只显示变化点；3天按4小时、7天按8小时取周期最大值。点或悬停任意点查看该点数量和时间。")
                        ManualSection("5. 出售", "勾选账号后点「批量出售」立即出售；「定时出售」由服务器到点执行，App/电脑关闭也有效。执行前请确认设备处于等待下一轮。")
                        ManualSection("6. 扫码登录", "账号详情 → 操作 → 扫码登录 → 选择二维码图片。图片里只能有一个二维码；上传后助手会切号并扫码。失败可点「停止扫码」。")
                        ManualSection("7. 设备控制", "设备详情可开启/关闭定时、停止脚本、同步 TIM 账号、最低/50% 亮度、远程重启、更新助手。操作前先确认助手在线且无障碍正常。")
                        ManualSection("8. 更新", "设置 → 版本更新可检查并安装客户端更新；设备详情 → 助手 → 更新助手可通知设备助手升级。更新会短暂重启，属正常。")
                        ManualSection("9. 数据不同步", "先点刷新；确认安全令牌有效；手机和电脑使用同一 API Key。仍异常时把设备码和账号发给管理员。")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun compareVer(a: String, b: String): Int {
    val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
    val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val x = pa.getOrElse(i) { 0 }
        val y = pb.getOrElse(i) { 0 }
        if (x != y) return x.compareTo(y)
    }
    return 0
}


@Composable
private fun ManualSection(title: String, body: String) {
    Column(Modifier.padding(vertical = 7.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Amber)
        Spacer(Modifier.height(3.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 20.sp)
        HorizontalDivider(color = Line, modifier = Modifier.padding(top = 8.dp))
    }
}


private fun serviceAddressValid(value: String): Boolean = runCatching {
    val uri = java.net.URI(value.trim())
    (uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)) && !uri.host.isNullOrBlank()
}.getOrDefault(false)
