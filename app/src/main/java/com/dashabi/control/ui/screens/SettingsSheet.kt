package com.dashabi.control.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.dashabi.control.data.CreditEntry
import com.dashabi.control.data.applySyncPayload
import com.dashabi.control.data.downloadSyncPayload
import com.dashabi.control.data.buildTransferData
import com.dashabi.control.data.parseTransferData
import com.dashabi.control.ui.vm.SyncViewModel
import com.dashabi.control.ui.vm.rememberVm
import com.dashabi.control.data.CloudSync
import com.dashabi.control.data.License
import com.dashabi.control.data.ApiResult
import com.dashabi.control.data.Me
import com.dashabi.control.data.ThemeMode
import com.dashabi.control.ui.components.InfoChip
import com.dashabi.control.ui.components.SectionTitle
import com.dashabi.control.ui.scopeLabel
import com.dashabi.control.ui.vm.RecognizeMode
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.Line
import com.dashabi.control.ui.theme.Mint
import com.dashabi.control.ui.theme.OnAccent
import com.dashabi.control.ui.theme.Rose
import com.dashabi.control.ui.theme.Surface1
import com.dashabi.control.ui.theme.Surface2
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.TextPrimary
import com.dashabi.control.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class TestResult(val ok: Boolean, val message: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    container: AppContainer,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onStartRecognition: (Boolean, RecognizeMode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val initial = container.settings.current()

    var key by remember { mutableStateOf(initial.apiKey) }
    var base by remember { mutableStateOf(initial.baseUrl) }
    var control by remember { mutableStateOf(container.settings.controlToken()) }
    var reveal by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TestResult?>(null) }
    var me by remember { mutableStateOf<Me?>(null) }

    val aiInitial = container.aiSettings.current()
    var aiKey by remember { mutableStateOf(aiInitial.apiKey) }
    var aiBase by remember { mutableStateOf(aiInitial.baseUrl) }
    var aiModel by remember { mutableStateOf(aiInitial.model) }
    var onlyMissing by remember { mutableStateOf(true) }
    val staminaLevels by container.stamina.state.collectAsState()
    val creditEntries by container.credit.state.collectAsState()
    val syncVm = rememberVm(SyncViewModel::class.java) { SyncViewModel(container.repo, container.stamina, container.credit) }
    val context = LocalContext.current
    val cloudSync = remember { CloudSync(context, container.stamina, container.credit, container.gameNames) }
    val license = remember { License(context) }
    var whitelisted by remember { mutableStateOf(false) }
    var deviceList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var deviceSortExpanded by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    val deviceOrder by container.deviceOrder.order.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        whitelisted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { license.whitelisted() }.getOrDefault(false)
        }
        val r = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            container.repo.accounts()
        }
        if (r is ApiResult.Ok) {
            deviceList = r.value.mapNotNull { a ->
                val code = a.deviceCode ?: return@mapNotNull null
                code to (a.deviceName ?: code)
            }.distinctBy { it.first }
        }
    }
    val clipboard = LocalClipboardManager.current
    var peerCode by remember { mutableStateOf(cloudSync.peerCode) }
    var cloudStatus by remember { mutableStateOf<String?>(null) }
    var cloudBusy by remember { mutableStateOf(false) }
    var updateBusy by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<String?>(null) }
    val updateScope = rememberCoroutineScope()
    fun checkUpdate() {
        if (updateBusy) return
        updateBusy = true
        updateStatus = "正在检查更新…"
        updateScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = runCatching {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://dasha-update-1313972671.cos.ap-shanghai.myqcloud.com/latest-android.json")
                    .header("User-Agent", "DashaControl/1.0 (Android)")
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val text = resp.body?.string().orEmpty()
                    val obj = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject
                    val version = (obj["version"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                    val url = (obj["url"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                    val sha = (obj["sha256"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                    Triple(version, url, sha)
                }
            }
            result.onSuccess { (version, url, sha) ->
                val current = BuildConfig.VERSION_NAME.substringBefore("-")
                if (compareVer(version, current) > 0) {
                    updateStatus = "发现新版本 $version，正在下载…"
                    val dl = runCatching {
                        val client = okhttp3.OkHttpClient()
                        val bytes = client.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { resp ->
                            if (!resp.isSuccessful) error("HTTP ${resp.code}")
                            resp.body?.bytes() ?: error("下载内容为空")
                        }
                        if (sha.isNotBlank()) {
                            val digest = java.security.MessageDigest.getInstance("SHA-256")
                            val actual = digest.digest(bytes).joinToString("") { "%02x".format(it) }
                            if (!actual.equals(sha, ignoreCase = true)) error("安装包校验失败")
                        }
                        val file = File(context.cacheDir, "update.apk")
                        file.writeBytes(bytes)
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                        val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        "已下载，正在启动安装…"
                    }
                    updateStatus = dl.getOrElse { "下载失败：${it.message}" }
                } else {
                    updateStatus = "已是最新版本 $current"
                }
            }.onFailure { updateStatus = "检查更新失败：${it.message}" }
            updateBusy = false
        }
    }
    val syncScope = rememberCoroutineScope()
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents != null) {
            syncing = true
            syncMessage = "正在读取同步数据…"
            syncScope.launch {
                val message = runCatching {
                    val json = withContext(Dispatchers.IO) { downloadSyncPayload(contents) }
                    val (levels, credits) = applySyncPayload(
                        json,
                        onStamina = { code, t, r, aw -> container.stamina.put(code, t, r, aw) },
                        onCredit = { code, score ->
                            container.credit.put(code, CreditEntry(score, null, null, System.currentTimeMillis()))
                        }
                    )
                    "同步完成：体力/AW $levels 条，信用分 $credits 条"
                }.getOrElse { "同步失败：${it.message}" }
                syncing = false
                syncMessage = message
            }
        }
    }
    var transferMessage by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            transferMessage = runCatching {
                val text = buildTransferData(staminaLevels, creditEntries)
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                "已导出：体力/AW ${staminaLevels.size} 条，信用分 ${creditEntries.size} 条"
            }.getOrElse { "导出失败：${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            transferMessage = runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("无法读取文件")
                val data = parseTransferData(text)
                data.stamina.forEach { (code, v) -> container.stamina.put(code, v.training, v.range, v.aw) }
                data.credit.forEach { (code, v) ->
                    if (v.score != null) container.credit.put(code, CreditEntry(v.score, v.nickname, null, v.updatedAt))
                }
                "已导入：体力/AW ${data.stamina.size} 条，信用分 ${data.credit.size} 条"
            }.getOrElse { "导入失败：${it.message}" }
        }
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
        if (save) {
            container.settings.save(key, base)
            container.settings.saveControlToken(control)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        contentColor = TextPrimary,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 22.dp)
        ) {
            Text("连接设置", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "开放 API · Bearer 鉴权 · 所有请求自动携带 User-Agent",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(Modifier.height(16.dp))

            SectionTitle("主题")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ThemeChip(
                        label = mode.label,
                        selected = themeMode == mode,
                        onClick = { container.theme.set(mode) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

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
            SectionTitle("API Key")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("zh_live_…", color = TextMuted) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { reveal = !reveal }) {
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
            SectionTitle("Base URL")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = base,
                onValueChange = { base = it },
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

            Spacer(Modifier.height(16.dp))
            Text("设备同步", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("本机设备码：${cloudSync.deviceCode}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(cloudSync.deviceCode))
                    cloudStatus = "设备码已复制"
                }) { Text("复制", style = MaterialTheme.typography.labelSmall, color = Amber) }
            }
            val trialEnd = license.trialStart() + 24 * 60 * 60 * 1000L
            if (!whitelisted && System.currentTimeMillis() < trialEnd) {
                Text(
                    "试用到期 ${com.dashabi.control.ui.formatTimestamp(trialEnd)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Rose
                )
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = peerCode,
                onValueChange = { peerCode = it; cloudSync.peerCode = it },
                label = { Text("对方设备码（电脑）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        if (!cloudBusy) {
                            cloudBusy = true
                            cloudStatus = "正在同步…"
                            updateScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                cloudStatus = cloudSync.sync()
                                cloudBusy = false
                            }
                        }
                    },
                    enabled = !cloudBusy,
                    shape = RoundedCornerShape(12.dp)
                ) { Text(if (cloudBusy) "同步中…" else "立即同步") }
            Spacer(Modifier.height(4.dp))
            com.dashabi.control.ui.vm.SyncStatus.last?.let {
                Text(
                    "上次同步：$it",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        updateScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val ok = runCatching { license.whitelisted() }.getOrDefault(false)
                            cloudStatus = if (ok) "已授权，重启 App 生效" else "未授权或已失效"
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("刷新授权") }
                Spacer(Modifier.width(8.dp))
                cloudStatus?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = TextMuted) }
            }

            Spacer(Modifier.height(16.dp))
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

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { showManual = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("使用手册", color = Amber) }

            if (showManual) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showManual = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = 620.dp)
                            .background(Surface1, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("使用手册", style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.weight(1f))
                            androidx.compose.material3.IconButton(onClick = { showManual = false }) {
                                Text("×", color = TextMuted, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            ManualSection("1. 首次使用", "设置里填 API Key → 填安全令牌 → 绑定云控助手设备码。绑定后设备详情才能远程重启、更新、亮度、扫码。")
                            ManualSection("2. 账号总览", "可按设备、状态、信用分筛选；点排序切换哈夫币/体力/AW/等级/资产。卡片上的「24h 峰值对比」是当前值对比昨日 6:00-今日 6:00 峰值，持平显示「持平」，没有数据显示「—」。")
                            ManualSection("3. 标注文字", "账号详情备注下方可填标注，保存后服务器同步；总览会显示在备注后面，也能被搜索。删除会同时删除服务器记录。")
                            ManualSection("4. 哈夫币变化图", "24小时只显示变化点；3天按4小时、7天按8小时取周期最大值。点或悬停任意点查看该点数量和时间。")
                            ManualSection("5. 出售", "勾选账号后点「批量出售」立即出售；「定时出售」由服务器到点执行，App/电脑关闭也有效。执行前请确认设备处于等待下一轮。")
                            ManualSection("6. 扫码登录", "账号详情 → 扫码登录 → 选择二维码图片。图片里只能有一个二维码；上传后助手会切号并扫码。失败可点「停止扫码」。")
                            ManualSection("7. 设备控制", "设备详情可开启/关闭定时、停止脚本、同步 TIM 账号、最低/50% 亮度、远程重启、更新助手。操作前先确认助手在线且无障碍正常。")
                            ManualSection("8. 更新", "手机/电脑设置里检查更新；助手在设备详情里「一键更新助手版本」。更新会短暂重启，属正常。")
                            ManualSection("9. 数据不同步", "先点刷新；确认安全令牌有效；手机和电脑使用同一 API Key。仍异常时把设备码和账号发给管理员。")
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }

            Text("软件更新", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { checkUpdate() }, enabled = !updateBusy, shape = RoundedCornerShape(12.dp)) {
                    Text(if (updateBusy) "请稍候…" else "检查更新")
                }
                Spacer(Modifier.width(8.dp))
                Text("当前版本 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
            updateStatus?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { runTest(true) },
                    enabled = !testing && key.isNotBlank() && base.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = OnAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存并测试", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = {
                        saveAi()
                        onSaved()
                    },
                    enabled = !testing,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Surface2,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("完成", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(4.dp))
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


            Spacer(Modifier.height(18.dp))
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = aiBase,
                    onValueChange = { aiBase = it },
                    modifier = Modifier.weight(1f),
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
                    modifier = Modifier.weight(0.9f),
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

            Spacer(Modifier.height(10.dp))
            Text(
                "Key 仅存在本机，卸载 App 即清除。",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Amber.copy(alpha = 0.15f) else Surface2)
            .border(1.dp, if (selected) Amber.copy(alpha = 0.45f) else Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Amber else TextSecondary
        )
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
