package com.dashabi.control.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dashabi.control.data.Account
import com.dashabi.control.data.ApiResult
import com.dashabi.control.data.AutoSellStore
import com.dashabi.control.data.Repository
import com.dashabi.control.ui.parseCompactNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 哈夫币低于 100 万的账号，每 2 小时（偶数整点）自动提交一次出售请求。
 *
 * 说明：这是 App 进程内的定时任务；App 被系统杀掉后不会在后台执行。
 * 结果写入 [AutoSellStore]，异常页卡片会显示请求时间与结果。
 */
class AutoSellViewModel(
    private val repo: Repository,
    private val store: AutoSellStore
) : ViewModel() {

    private var selling = false

    init {
        viewModelScope.launch {
            while (true) {
                runCatching { tick() }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun tick() {
        if (selling) return
        val now = System.currentTimeMillis()
        val slot = slotStart(now)
        // 只在整点后的 10 分钟内执行，避免 App 中途打开时错过这一轮
        if (now - slot > WINDOW_MS) return
        if (store.lastSlot() >= slot) return

        val accounts = when (val result = repo.accounts()) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> return
        }
        store.setLastSlot(slot)

        val targets = accounts.filter { isAutoSellTarget(it) }
        if (targets.isEmpty()) return

        selling = true
        try {
            targets.forEach { account ->
                val device = account.deviceCode ?: return@forEach
                when (val result = repo.sellOne(device, account.accountCode)) {
                    is ApiResult.Ok -> store.record(account.accountCode, true, "已提交")
                    is ApiResult.Err ->
                        store.record(account.accountCode, false, result.message.take(80))
                }
                delay(400)
            }
        } finally {
            selling = false
        }
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 30_000L
        private const val WINDOW_MS = 10 * 60 * 1000L

        /** 当前所在的 2 小时整点（本地时间 00:00 / 02:00 / …）。 */
        fun slotStart(now: Long): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            cal.set(Calendar.HOUR_OF_DAY, hour - hour % 2)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        /** 哈夫币 < 100 万、且有设备可以提交出售。 */
        fun isAutoSellTarget(account: Account): Boolean {
            if (account.deviceCode.isNullOrBlank()) return false
            // 暂停中的账号不自动出售
            if (account.paused || account.status == "paused") return false
            val haf = parseCompactNumber(account.hafCoin) ?: return false
            return haf < 1_000_000.0
        }
    }
}
