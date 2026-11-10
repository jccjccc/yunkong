package com.dashabi.control.remote

import android.content.Context
import org.json.JSONObject

/**
 * 「云控设备 → 云控助手设备码」的绑定表，带时间戳，随同步文件和体力/信用分一样双向合并。
 */
object RebootBindStore {
    private const val PREFS = "dasha_reboot_bind"
    private const val KEY = "map"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun ensureCleared(context: Context) {
        val p = prefs(context)
        if (p.getBoolean("cleared_v142", false)) return
        p.edit()
            .putString(KEY, "{}")
            .putBoolean("cleared_v142", true)
            .putLong("clear_before", System.currentTimeMillis())
            .apply()
    }

    private fun clearBefore(context: Context): Long =
        prefs(context).getLong("clear_before", 0L)

    private fun load(context: Context): JSONObject {
        ensureCleared(context)
        val raw = prefs(context).getString(KEY, null)
        return runCatching { JSONObject(raw ?: "{}") }.getOrDefault(JSONObject())
    }

    fun get(context: Context, device: String): String? {
        val o = load(context).optJSONObject(device) ?: return null
        return o.optString("code").takeIf { it.isNotBlank() }
    }

    fun put(context: Context, device: String, code: String) {
        val obj = load(context)
        obj.put(device, JSONObject().put("code", code.trim()).put("ts", System.currentTimeMillis()))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, obj.toString()).apply()
    }

    fun remove(context: Context, device: String) {
        val obj = load(context)
        obj.remove(device)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, obj.toString()).apply()
    }

    /** 供同步文件用：{设备: {"code":..., "ts":...}} */
    fun toJson(context: Context): JSONObject = load(context)

    /** 合并对端绑定表，按 ts 新的覆盖旧的。 */
    fun merge(context: Context, incoming: JSONObject?) {
        if (incoming == null) return
        val local = load(context)
        incoming.keys().forEach { device ->
            val inc = incoming.optJSONObject(device) ?: return@forEach
            val code = inc.optString("code")
            val ts = inc.optLong("ts", 0L)
            if (code.isBlank()) return@forEach
            if (ts > 0 && ts < clearBefore(context)) return@forEach
            val cur = local.optJSONObject(device)
            val curTs = cur?.optLong("ts", 0L) ?: 0L
            if (cur == null || ts >= curTs) {
                local.put(device, JSONObject().put("code", code).put("ts", ts))
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, local.toString()).apply()
    }
}
