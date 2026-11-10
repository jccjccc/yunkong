package com.dashabi.control.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dashabi.control.data.AbnormalNotifier
import com.dashabi.control.data.Account
import com.dashabi.control.data.ApiResult
import com.dashabi.control.data.AutoSellStore
import com.dashabi.control.data.Repository
import com.dashabi.control.ui.parseCompactNumber
import kotlinx.coroutines.Job
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import kotlinx.coroutines.launch

/** 一条异常记录：账号 + 命中的异常原因。 */
data class AbnormalAccount(
    val account: Account,
    val reasons: List<String>,
    val hafCoin: Double?
)

/**
 * 异常账号页的数据来源。
 *
 * 判定规则（与账号总览里的「异常」筛选相互独立）：
 *  - 云端状态为 abnormal
 *  - 哈夫币低于 100 万（或数值无法解析）
 *  - 任意设施进度缺少完成时间
 */
class AbnormalViewModel(
    private val repo: Repository,
    private val notifier: AbnormalNotifier,
    private val autoSell: AutoSellStore
) : ViewModel() {

    var items by mutableStateOf<List<AbnormalAccount>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var lastUpdated by mutableStateOf<Long?>(null); private set

    private var job: Job? = null
    private var knownAccounts: Map<String, Account>? = null

    fun load(silent: Boolean = false) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            if (!silent) loading = true
            when (val result = repo.accounts()) {
                is ApiResult.Ok -> {
                    val fresh = result.value.mapNotNull { account ->
                        val reasons = abnormalReasons(account)
                        if (reasons.isEmpty()) null
                        else AbnormalAccount(account, reasons, parseCompactNumber(account.hafCoin))
                    }
                    val previous = knownAccounts
                    val current = fresh.map { it.account }.associateBy { it.accountCode }
                    knownAccounts = current
                    items = fresh.sortedWith(
                        compareBy(
                            { entryPriority(it.reasons) },
                            { it.hafCoin ?: 0.0 }
                        )
                    )
                    lastUpdated = System.currentTimeMillis()
                    error = null

                    if (previous != null) {
                        val added = newAbnormal(previous.keys, fresh)
                        if (added.isNotEmpty()) notifier.notifyNew(added)

                        val recovered = previous.keys - current.keys
                        val recoveredAccounts = recovered
                            .mapNotNull { previous[it] }
                            .filter { account ->
                                // 刚被自动出售成功的账号会从列表消失，不当作「异常解除」提醒
                                val record = autoSell.get(account.accountCode)
                                val soldRecently = record != null && record.success &&
                                    System.currentTimeMillis() - record.lastAttemptAt < 12 * 60 * 60 * 1000L
                                !soldRecently
                            }
                        if (recoveredAccounts.isNotEmpty()) {
                            notifier.notifyRecovered(recoveredAccounts)
                        }
                    }
                }
                is ApiResult.Err -> error = result.message
            }
            loading = false
        }
    }

    companion object {
        private const val HAF_COIN_THRESHOLD = 1_000_000.0
        private const val OVERDUE_MS = 10 * 60 * 1000L

        /** 兼容带时区 / 不带时区 / 空格分隔的完成时间。 */
        private fun parseFacilityTime(raw: String?): Long? {
            if (raw.isNullOrBlank()) return null
            val text = raw.trim()
            return runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrNull()
                ?: runCatching {
                    LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.getOrNull()
                ?: runCatching {
                    // 接口列表里的格式是「10-30 05:02」，没有年份：按最近的年份推断
                    val now = ZonedDateTime.now()
                    val formatter = DateTimeFormatterBuilder()
                        .appendPattern("MM-dd HH:mm")
                        .parseDefaulting(ChronoField.YEAR, now.year.toLong())
                        .toFormatter()
                    var value = LocalDateTime.parse(text, formatter)
                        .atZone(ZoneId.systemDefault())
                    if (value.isBefore(now.minusDays(1))) value = value.plusYears(1)
                    if (value.isAfter(now.plusDays(1))) value = value.minusYears(1)
                    value.toInstant().toEpochMilli()
                }.getOrNull()
        }

        /** 第一次加载只建立基线，不提醒；之后对比上一次的账号集合。 */
        fun newAbnormal(previous: Set<String>?, current: List<AbnormalAccount>): List<AbnormalAccount> =
            if (previous == null) emptyList()
            else current.filter { it.account.accountCode !in previous }

        fun abnormalReasons(account: Account): List<String> {
            val reasons = mutableListOf<String>()

            if (account.status == "abnormal") reasons += "云端异常"

            val raw = account.rawStatus.orEmpty()
            val statusText = account.status.orEmpty()
            val frozen = raw.contains("冻结") || statusText.contains("冻结") ||
                statusText.equals("frozen", ignoreCase = true)
            val blueFace = raw.contains("蓝脸")
            val faceId = !blueFace && raw.contains("人脸")
            val needsVerify = blueFace || faceId || raw.contains("验证")

            if (frozen) reasons += "账号冻结"
            if (blueFace) reasons += "蓝脸验证"
            else if (faceId) reasons += "人脸验证"
            else if (needsVerify) reasons += "需要验证"

            val haf = parseCompactNumber(account.hafCoin)
            when {
                account.hafCoin.isNullOrBlank() -> reasons += "哈夫币缺失"
                haf == null -> reasons += "哈夫币数据异常"
                haf < HAF_COIN_THRESHOLD -> reasons += "哈夫币不足"
            }

            // 设施进度只在账号状态正常（未暂停、非设备停止）时判断；
            // 暂停/停止的账号设施时间本来就可能是旧的。
            val checkFacilities = account.status == "normal" && !account.paused
            val facilities = account.facilities
            if (!checkFacilities) {
                // 跳过设施判断
            } else if (facilities == null) {
                reasons += "设施数据缺失"
            } else {
                val entries = listOf(
                    "技术中心" to facilities.tech,
                    "工作台" to facilities.workbench,
                    "制药台" to facilities.pharmacy,
                    "防具台" to facilities.armory
                )
                val now = System.currentTimeMillis()
                entries.forEach { (label, facility) ->
                    val finish = parseFacilityTime(facility?.finishAt)
                    when {
                        facility == null || facility.finishAt.isNullOrBlank() ->
                            reasons += "$label 无完成时间"
                        finish == null ->
                            reasons += "$label 无完成时间"
                        now - finish > OVERDUE_MS ->
                            reasons += "$label 超时"
                    }
                }
            }

            // 展示优先级：工作台超时 > 其他异常 > 哈夫币不足
            return reasons.sortedBy { reasonPriority(it) }
        }

        /** 异常账号列表顺序：工作台超时 > 其他异常 > 只有哈夫币不足。 */
        private fun entryPriority(reasons: List<String>): Int = when {
            reasons.any { it.endsWith("超时") } -> 0
            reasons.all { it.startsWith("哈夫币") } -> 2
            else -> 1
        }

        private fun reasonPriority(reason: String): Int = when {
            reason.endsWith("超时") -> 0
            reason.startsWith("哈夫币") -> 2
            else -> 1
        }
    }
}
