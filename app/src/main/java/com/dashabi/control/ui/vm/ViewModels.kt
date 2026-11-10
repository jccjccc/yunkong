package com.dashabi.control.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dashabi.control.data.AbnormalNotifier
import com.dashabi.control.data.Account
import com.dashabi.control.data.ApiResult
import com.dashabi.control.data.Device
import com.dashabi.control.data.DeviceStatus
import com.dashabi.control.data.Images
import com.dashabi.control.data.Repository
import com.dashabi.control.data.StaminaStore
import com.dashabi.control.data.Summary
import com.dashabi.control.ui.deviceStatusStyle
import com.dashabi.control.ui.extractMessage
import com.dashabi.control.ui.jsonBoolean
import com.dashabi.control.ui.jsonString
import com.dashabi.control.ui.matchesSearch
import com.dashabi.control.ui.naturalCompare
import com.dashabi.control.ui.parseCompactNumber
import com.dashabi.control.ui.searchRelevance
import com.dashabi.control.ui.statusKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/* ------------------------------------------------------------------ */
/*  设备总览                                                            */
/* ------------------------------------------------------------------ */

class DevicesViewModel(
    private val repo: Repository,
    private val notifier: AbnormalNotifier
) : ViewModel() {

    var devices by mutableStateOf<List<Device>>(emptyList()); private set
    var summary by mutableStateOf<Summary?>(null); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var lastUpdated by mutableStateOf<Long?>(null); private set
    var abnormalCount by mutableStateOf(0); private set

    private var job: Job? = null
    private var knownAbnormal: Set<String>? = null

    fun load(silent: Boolean = false) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            if (!silent) loading = true
            val summaryResult = repo.summary()
            val devicesResult = repo.devices()
            when (summaryResult) {
                is ApiResult.Ok -> summary = summaryResult.value
                is ApiResult.Err -> if (devicesResult is ApiResult.Err) error = summaryResult.message
            }
            when (devicesResult) {
                is ApiResult.Ok -> {
                    // 刷新设备总览时逐个读取实时状态，卡片显示的就是当前状态
                    val merged = withLiveStatuses(devicesResult.value)
                    devices = merged
                    val current = merged.filter { deviceAbnormalReasons(it.status).isNotEmpty() }
                    abnormalCount = current.size
                    val previous = knownAbnormal
                    knownAbnormal = current.map { it.deviceCode }.toSet()
                    if (previous != null) {
                        val added = current.filter { it.deviceCode !in previous }
                        if (added.isNotEmpty()) notifier.notifyAbnormalDevices(added)
                    }
                    lastUpdated = System.currentTimeMillis()
                    error = null
                }
                is ApiResult.Err -> if (devices.isEmpty()) error = devicesResult.message
            }
            loading = false
        }
    }

    /** 并发读取每台设备的实时状态（/devices/{code}/status），失败时保留列表里的旧状态。 */
    private suspend fun withLiveStatuses(source: List<Device>): List<Device> = coroutineScope {
        val semaphore = Semaphore(4)
        source.map { device ->
            async {
                semaphore.withPermit {
                    val code = device.deviceCode
                    if (code.isBlank()) return@withPermit device
                    when (val live = withTimeoutOrNull(15_000) { repo.deviceLive(code) }) {
                        is ApiResult.Ok -> live.value.status?.let { device.copy(status = it) } ?: device
                        else -> device
                    }
                }
            }
        }.awaitAll()
    }
}

/* ------------------------------------------------------------------ */
/*  账号总览                                                            */
/* ------------------------------------------------------------------ */

enum class AccountSort(val label: String) {
    HAF_COIN("哈夫币"),
    STAMINA("体力"),
    AW("AW"),
    ACTION_LEVEL("行动等级"),
    TOTAL_ASSET("总资产")
}

class AccountsViewModel(
    private val repo: Repository,
    private val staminaStore: StaminaStore,
    private val hafHistory: com.dashabi.control.data.HafHistoryStore,
    private val deviceOrder: com.dashabi.control.data.DeviceOrderStore,
    private val cloudSync: com.dashabi.control.data.CloudSync
) : ViewModel() {

    var accounts by mutableStateOf<List<Account>>(emptyList()); private set
    var hafMaxMap by mutableStateOf<Map<String, Double>>(emptyMap()); private set
    var annotations by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var lastUpdated by mutableStateOf<Long?>(null); private set
    var query by mutableStateOf(""); private set
    var deviceFilter by mutableStateOf<String?>(null); private set
    var statusFilter by mutableStateOf("all"); private set
    var creditFilter by mutableStateOf("all"); private set
    var sort by mutableStateOf(AccountSort.HAF_COIN); private set
    var sortDesc by mutableStateOf(true); private set

    private var job: Job? = null

    fun load(silent: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            if (!silent) loading = true
            val result = repo.accounts()
            if (!isActive) return@launch
            when (result) {
                is ApiResult.Ok -> {
                    accounts = result.value
                    result.value.forEach { hafHistory.record(it.accountCode, it.hafCoin) }
                    val bucket = System.currentTimeMillis() / 300_000L * 300_000L
                    val snaps = result.value.mapNotNull { a ->
                        val v = com.dashabi.control.ui.parseCompactNumber(a.hafCoin) ?: return@mapNotNull null
                        if (v <= 0 || !hafHistory.shouldUpload(a.accountCode, v)) null
                        else Triple(a.accountCode, bucket, v)
                    }
                    if (snaps.isNotEmpty() && cloudSync.recordHaf(snaps)) {
                        snaps.forEach { hafHistory.markUploaded(it.first, it.third) }
                    }
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 6)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    if (cal.timeInMillis > System.currentTimeMillis()) cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
                    hafMaxMap = cloudSync.hafMaxSince(cal.timeInMillis - 24 * 3600_000L, cal.timeInMillis)
                    annotations = cloudSync.getAnnotations()
                    lastUpdated = System.currentTimeMillis()
                    error = null
                    if (statusFilter != "all" && result.value.none { it.statusKey == statusFilter }) {
                        statusFilter = "all"
                    }
                }
                is ApiResult.Err -> error = result.message
            }
            loading = false
        }
    }

    /** 搜索在本地完成：账号已全部加载，输入后立即过滤，不再请求后端。 */
    fun setSearch(text: String) {
        val normalized = text.trim()
        if (normalized != query) query = normalized
    }

    fun selectDevice(code: String?) {
        deviceFilter = code
    }

    fun selectStatus(key: String) {
        statusFilter = key
    }

    fun selectCredit(key: String) {
        creditFilter = key
    }

    /** 手动刷新时重置筛选和排序。 */
    fun resetFiltersForRefresh() {
        statusFilter = "all"
        creditFilter = "all"
        deviceFilter = null
        sort = AccountSort.HAF_COIN
        sortDesc = true
    }

    /** 点击搜索栏时把筛选重置为「全部 / 全部设备」。 */
    fun resetFiltersForSearch() {
        if (statusFilter != "all") statusFilter = "all"
        if (deviceFilter != null) deviceFilter = null
        if (creditFilter != "all") creditFilter = "all"
    }

    /** 选中某项排序；再次点击同一项切换升/降序。 */
    fun selectSort(key: AccountSort) {
        if (sort == key) {
            sortDesc = !sortDesc
        } else {
            sort = key
            sortDesc = true
        }
    }

    fun refresh() = load()

    val deviceOptions: List<Pair<String, String>>
        get() {
            val order = deviceOrder.order.value
            val base = accounts
                .mapNotNull { a ->
                    val code = a.deviceCode ?: return@mapNotNull null
                    code to (a.deviceName ?: code)
                }
                .distinctBy { it.first }
            return base.sortedWith { x, y ->
                val xi = order.indexOf(x.first)
                val yi = order.indexOf(y.first)
                when {
                    xi >= 0 && yi >= 0 -> xi.compareTo(yi)
                    xi >= 0 -> -1
                    yi >= 0 -> 1
                    else -> naturalCompare(x.second, y.second)
                }
            }
        }

    private val deviceScoped: List<Account>
        get() = deviceFilter?.let { code -> accounts.filter { it.deviceCode == code } } ?: accounts

    private val searchScoped: List<Account>
        get() {
            val keyword = query
            if (keyword.isBlank()) return deviceScoped
            return deviceScoped.filter { it.matchesSearch(keyword) || (annotations[it.accountCode]?.contains(keyword, ignoreCase = true) == true) }
        }



    fun statusCount(key: String): Int =
        if (key == "all") searchScoped.size else searchScoped.count { it.statusKey == key }

    private val filteredScoped: List<Account>
        get() {
            val scoped = searchScoped
            return if (statusFilter == "all") scoped else scoped.filter { it.statusKey == statusFilter }
        }

    /** 当前筛选 / 搜索条件下的哈夫币总和（真实数值，展示时再换算成万）。 */
    val visibleHafCoinTotal: Double
        get() = filteredScoped.sumOf { parseCompactNumber(it.hafCoin) ?: 0.0 }

    val visibleAccounts: List<Account>
        get() {
            val filtered = filteredScoped

            val metric: (Account) -> Double? = when (sort) {
                AccountSort.ACTION_LEVEL -> { account -> parseCompactNumber(account.actionLevel) }
                AccountSort.HAF_COIN -> { account -> parseCompactNumber(account.hafCoin) }
                AccountSort.STAMINA -> { account -> staminaStore.get(account.accountCode)?.training?.toDouble() }
                AccountSort.AW -> { account -> staminaStore.get(account.accountCode)?.awBullets?.toDouble() }
                AccountSort.TOTAL_ASSET -> { account -> parseCompactNumber(account.totalAsset) }
            }
            return filtered.sortedWith { x, y ->
                // 搜索时相关度优先，相关度相同再按当前排序规则
                val byRelevance = if (query.isBlank()) {
                    0
                } else {
                    y.searchRelevance(query).compareTo(x.searchRelevance(query))
                }
                if (byRelevance != 0) return@sortedWith byRelevance

                val vx = metric(x)
                val vy = metric(y)
                when {
                    vx == null && vy == null -> 0
                    vx == null -> 1
                    vy == null -> -1
                    sortDesc -> vy.compareTo(vx)
                    else -> vx.compareTo(vy)
                }
            }
        }
}

/* ------------------------------------------------------------------ */
/*  设备详情                                                            */
/* ------------------------------------------------------------------ */

class DeviceDetailViewModel(
    private val repo: Repository,
    val deviceCode: String
) : ViewModel() {

    var device by mutableStateOf<Device?>(null); private set
    var liveStatus by mutableStateOf<DeviceStatus?>(null); private set
    var accounts by mutableStateOf<List<Account>>(emptyList()); private set
    var sellStatus by mutableStateOf<JsonElement?>(null); private set
    var loading by mutableStateOf(false); private set
    var lastUpdated by mutableStateOf<Long?>(null); private set
    var error by mutableStateOf<String?>(null); private set
    var selected by mutableStateOf<Set<String>>(emptySet()); private set
    var sellPolling by mutableStateOf(false); private set
    var busyAction by mutableStateOf<String?>(null); private set

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private var loadJob: Job? = null
    private var pollJob: Job? = null

    fun load(silent: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            if (!silent) loading = true
            val d = repo.device(deviceCode)
            val a = repo.deviceAccounts(deviceCode)
            when (d) {
                is ApiResult.Ok -> {
                    device = d.value
                    liveStatus = d.value.status
                }
                is ApiResult.Err -> if (!silent) error = d.message
            }
            when (a) {
                is ApiResult.Ok -> {
                    accounts = a.value
                    selected = selected.intersect(a.value.map { it.accountCode }.toSet())
                }
                is ApiResult.Err -> error = a.message
            }
            lastUpdated = System.currentTimeMillis()
            loading = false
        }
    }

    fun refreshLive() {
        viewModelScope.launch {
            busyAction = "live"
            when (val r = repo.deviceLive(deviceCode)) {
                is ApiResult.Ok -> {
                    liveStatus = r.value.status
                    _messages.emit(UiMessage("实时状态：${deviceStatusStyle(r.value.status).label}"))
                }
                is ApiResult.Err -> _messages.emit(UiMessage(r.message, true))
            }
            busyAction = null
        }
    }

    fun toggleTimer(enable: Boolean) = runAction(
        name = "timer",
        fallback = if (enable) "定时任务已开启" else "定时任务已关闭"
    ) { if (enable) repo.timerEnable(deviceCode) else repo.timerDisable(deviceCode) }

    fun stop(disableTimer: Boolean) = runAction("stop", "已请求停止设备脚本") {
        repo.stop(deviceCode, disableTimer)
    }

    fun sync() = runAction("sync", "同步请求已提交") { repo.sync(deviceCode) }

    fun pause(account: Account) = runAction("pause-${account.accountCode}", "已暂停 ${account.gameNick ?: account.accountCode}") {
        repo.pause(deviceCode, account.accountCode)
    }

    fun resume(account: Account) = runAction("resume-${account.accountCode}", "已恢复 ${account.gameNick ?: account.accountCode}") {
        repo.resume(deviceCode, account.accountCode)
    }

    fun runProduction(account: Account) = runAction("prod-${account.accountCode}", "已提交制造任务") {
        repo.runProduction(deviceCode, account.accountCode)
    }

    fun sellOne(account: Account) {
        viewModelScope.launch {
            busyAction = "sell-${account.accountCode}"
            when (val r = repo.sellOne(deviceCode, account.accountCode)) {
                is ApiResult.Ok -> {
                    _messages.emit(UiMessage("出售请求已受理，正在跟踪状态"))
                    startSellPolling()
                }
                is ApiResult.Err -> _messages.emit(UiMessage(r.message, true))
            }
            busyAction = null
        }
    }

    fun sellSelected() {
        val chosen = accounts.filter { it.accountCode in selected }
        val ids = chosen.mapNotNull { it.accountCode.toLongOrNull() }
        if (ids.isEmpty()) {
            viewModelScope.launch { _messages.emit(UiMessage("所选账号没有可用的数字 ID，无法批量出售", true)) }
            return
        }
        viewModelScope.launch {
            busyAction = "sell-batch"
            when (val r = repo.sellBatch(deviceCode, ids)) {
                is ApiResult.Ok -> {
                    _messages.emit(UiMessage("已提交 ${ids.size} 个账号的出售，正在跟踪状态"))
                    clearSelection()
                    startSellPolling()
                }
                is ApiResult.Err -> _messages.emit(UiMessage(r.message, true))
            }
            busyAction = null
        }
    }

    fun loadSellStatus(showMessage: Boolean = false) {
        viewModelScope.launch {
            when (val r = repo.sellStatus(deviceCode)) {
                is ApiResult.Ok -> sellStatus = r.value
                is ApiResult.Err -> if (showMessage) _messages.emit(UiMessage(r.message, true))
            }
        }
    }

    fun startSellPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            sellPolling = true
            var seenActive = false
            var ticks = 0
            while (isActive && ticks < 72) {
                delay(2500)
                ticks++
                when (val r = repo.sellStatus(deviceCode)) {
                    is ApiResult.Ok -> {
                        sellStatus = r.value
                        val active = jsonBoolean(r.value, "active")
                        if (active == true) seenActive = true
                        if (active == false && (seenActive || ticks >= 4)) break
                    }
                    is ApiResult.Err -> Unit
                }
            }
            sellPolling = false
            load(silent = true)
        }
    }

    fun toggleSelection(code: String) {
        selected = if (code in selected) selected - code else selected + code
    }

    fun selectAll() {
        selected = accounts.map { it.accountCode }.toSet()
    }

    fun clearSelection() {
        selected = emptySet()
    }

    private fun runAction(name: String, fallback: String, block: suspend () -> ApiResult<String>) {
        viewModelScope.launch {
            busyAction = name
            when (val r = block()) {
                is ApiResult.Ok -> {
                    _messages.emit(UiMessage(extractMessage(r.value) ?: fallback))
                    load(silent = true)
                }
                is ApiResult.Err -> _messages.emit(UiMessage(r.message, true))
            }
            busyAction = null
        }
    }
}

/* ------------------------------------------------------------------ */
/*  账号详情                                                            */
/* ------------------------------------------------------------------ */

class AccountDetailViewModel(
    private val repo: Repository,
    val deviceCode: String,
    val accountCode: String
) : ViewModel() {

    var account by mutableStateOf<Account?>(null); private set
    var images by mutableStateOf<Images?>(null); private set
    var schedules by mutableStateOf<List<JsonObject>>(emptyList()); private set
    var sellStatus by mutableStateOf<JsonElement?>(null); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var sellPolling by mutableStateOf(false); private set
    var busyAction by mutableStateOf<String?>(null); private set

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private var loadJob: Job? = null
    private var pollJob: Job? = null

    fun load(silent: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            if (!silent) loading = true
            when (val r = repo.account(deviceCode, accountCode)) {
                is ApiResult.Ok -> {
                    account = r.value
                    error = null
                }
                is ApiResult.Err -> error = r.message
            }
            when (val r = repo.accountImages(deviceCode, accountCode)) {
                is ApiResult.Ok -> images = r.value
                is ApiResult.Err -> Unit
            }
            loadSchedulesInternal()
            loading = false
        }
    }

    fun refresh() = load()

    fun pause() {
        val acc = account ?: return
        runAction("pause", "已暂停 ${acc.gameNick ?: acc.accountCode}") { repo.pause(deviceCode, accountCode) }
    }

    fun resume() {
        val acc = account ?: return
        runAction("resume", "已恢复 ${acc.gameNick ?: acc.accountCode}") { repo.resume(deviceCode, accountCode) }
    }

    fun runProduction() {
        runAction("prod", "已提交制造任务") { repo.runProduction(deviceCode, accountCode) }
    }

    fun sell() {
        viewModelScope.launch {
            busyAction = "sell"
            when (val r = repo.sellOne(deviceCode, accountCode)) {
                is ApiResult.Ok -> {
                    _messages.emit(UiMessage("出售请求已受理，正在跟踪状态"))
                    startSellPolling()
                }
                is ApiResult.Err -> _messages.emit(UiMessage(r.message, true))
            }
            busyAction = null
        }
    }

    fun saveNote(note: String) {
        viewModelScope.launch {
            busyAction = "note"
            when (val r = repo.setNote(deviceCode, accountCode, note)) {
                is ApiResult.Ok -> {
                    _messages.emit(UiMessage("备注已保存"))
                    load(silent = true)
                }
                is ApiResult.Err -> _messages.emit(UiMessage(r.message, true))
            }
            busyAction = null
        }
    }

    fun createSchedule(startAt: String, endAt: String) {
        val acc = account
        viewModelScope.launch {
            busyAction = "schedule"
            when (val r = repo.createSchedule(deviceCode, accountCode, acc?.gameNick, startAt, endAt)) {
                is ApiResult.Ok -> {
                    _messages.emit(UiMessage(extractMessage(r.value) ?: "定时暂停计划已创建"))
                    loadSchedulesInternal()
                }
                is ApiResult.Err -> _messages.emit(UiMessage(r.message, true))
            }
            busyAction = null
        }
    }

    fun cancelSchedule(id: Long) {
        viewModelScope.launch {
            busyAction = "cancel-schedule-$id"
            when (val r = repo.cancelSchedule(id)) {
                is ApiResult.Ok -> {
                    _messages.emit(UiMessage(extractMessage(r.value) ?: "定时暂停计划已取消"))
                    loadSchedulesInternal()
                }
                is ApiResult.Err -> _messages.emit(UiMessage(r.message, true))
            }
            busyAction = null
        }
    }

    fun loadSellStatus() {
        viewModelScope.launch {
            when (val r = repo.sellStatus(deviceCode)) {
                is ApiResult.Ok -> sellStatus = r.value
                is ApiResult.Err -> Unit
            }
        }
    }

    fun startSellPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            sellPolling = true
            var seenActive = false
            var ticks = 0
            while (isActive && ticks < 72) {
                delay(2500)
                ticks++
                when (val r = repo.sellStatus(deviceCode)) {
                    is ApiResult.Ok -> {
                        sellStatus = r.value
                        val active = jsonBoolean(r.value, "active")
                        if (active == true) seenActive = true
                        if (active == false && (seenActive || ticks >= 4)) break
                    }
                    is ApiResult.Err -> Unit
                }
            }
            sellPolling = false
            load(silent = true)
        }
    }

    private suspend fun loadSchedulesInternal() {
        when (val r = repo.schedules(deviceCode)) {
            is ApiResult.Ok -> {
                val nick = account?.gameNick
                val all = r.value
                val filtered = all.filter { obj ->
                    val code = jsonString(obj, "account_code")
                    val name = jsonString(obj, "account_name")
                    code == accountCode || (code == null && !nick.isNullOrBlank() && name == nick)
                }
                // 若接口未返回 account_code，则退化为展示该设备下的全部计划
                schedules = if (filtered.isEmpty()) all else filtered
            }
            is ApiResult.Err -> Unit
        }
    }

    private fun runAction(name: String, fallback: String, block: suspend () -> ApiResult<String>) {
        viewModelScope.launch {
            busyAction = name
            when (val r = block()) {
                is ApiResult.Ok -> {
                    _messages.emit(UiMessage(extractMessage(r.value) ?: fallback))
                    load(silent = true)
                }
                is ApiResult.Err -> _messages.emit(UiMessage(r.message, true))
            }
            busyAction = null
        }
    }
}
/** 设备异常：关闭定时 / 已停止 / 无障碍关闭。 */
fun deviceAbnormalReasons(status: DeviceStatus?): List<String> {
    if (status == null) return emptyList()
    val reasons = mutableListOf<String>()
    if (status.timerEnabled == false) reasons += "定时已关闭"
    if (status.online == false) reasons += "设备离线"
    when (status.code) {
        "stopped" -> reasons += "已停止"
    }
    return reasons
}