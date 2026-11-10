package com.dashabi.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** 记录每个账号的哈夫币快照，按早上 6 点到次日 6 点统计 24 小时峰值。 */
class HafHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("haf_history", Context.MODE_PRIVATE)
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun record(accountCode: String, raw: String?) {
        val value = com.dashabi.control.ui.parseCompactNumber(raw) ?: return
        if (value <= 0) return
        val now = System.currentTimeMillis()
        val all = load()
        val list = all.optJSONArray(accountCode) ?: JSONArray()
        val last = if (list.length() > 0) list.optJSONObject(list.length() - 1) else null
        // 5 分钟内且值没变，不重复记
        if (last != null && now - last.optLong("t", 0L) < 5 * 60_000L &&
            kotlin.math.abs(last.optDouble("v", 0.0) - value) < 1.0
        ) return
        list.put(JSONObject().put("t", now).put("v", value))
        val pruned = JSONArray()
        val cutoff = now - 7 * 24 * 3600_000L
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            if (o.optLong("t", 0) >= cutoff) pruned.put(o)
        }
        all.put(accountCode, pruned)
        prefs.edit().putString("data", all.toString()).apply()
        _version.value++
    }

    /** 指定时间范围内的点，按时间升序。 */
    fun points(accountCode: String, since: Long): List<Pair<Long, Double>> {
        val list = load().optJSONArray(accountCode) ?: return emptyList()
        val out = ArrayList<Pair<Long, Double>>()
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val t = o.optLong("t", 0)
            val v = o.optDouble("v", Double.NaN)
            if (t >= since && !v.isNaN()) out.add(t to v)
        }
        out.sortBy { it.first }
        return out
    }

    /** 只有哈夫币发生变化才上传服务器，24 小时图只保留变化点。 */
    fun shouldUpload(accountCode: String, value: Double): Boolean {
        val last = uploaded().optDouble(accountCode, Double.NaN)
        return last.isNaN() || kotlin.math.abs(last - value) >= 1.0
    }

    fun markUploaded(accountCode: String, value: Double) {
        val obj = uploaded().put(accountCode, value)
        prefs.edit().putString("uploads", obj.toString()).apply()
    }

    private fun uploaded(): JSONObject = runCatching {
        JSONObject(prefs.getString("uploads", "{}") ?: "{}")
    }.getOrDefault(JSONObject())

    /** 当前账号在「今天/昨天 6 点起的 24 小时窗口」内的最大哈夫币。 */
    fun maxInWindow(accountCode: String, now: Long = System.currentTimeMillis()): Double? {
        val list = load().optJSONArray(accountCode) ?: return null
        val start = windowStart(now)
        var max: Double? = null
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val t = o.optLong("t", 0)
            if (t < start || t > now) continue
            val v = o.optDouble("v", Double.NaN)
            if (!v.isNaN() && (max == null || v > max!!)) max = v
        }
        return max
    }

    /** 当前值 - 窗口最大值，正数显示 +，负数显示 -。 */
    fun deltaText(accountCode: String, raw: String?, now: Long = System.currentTimeMillis()): String? {
        val current = com.dashabi.control.ui.parseCompactNumber(raw) ?: return null
        val max = maxInWindow(accountCode, now) ?: return null
        val delta = current - max
        if (kotlin.math.abs(delta) < 1.0) return "持平"
        val text = com.dashabi.control.ui.formatWanAmount(kotlin.math.abs(delta))
        return if (delta > 0) "+$text" else "-$text"
    }

    private fun windowStart(now: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis > now) cal.add(Calendar.DAY_OF_MONTH, -1)
        return cal.timeInMillis
    }

    private fun load(): JSONObject = runCatching {
        JSONObject(prefs.getString("data", "{}") ?: "{}")
    }.getOrDefault(JSONObject())
}
