package com.dashabi.control.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/** 设备定时出售：保存勾选账号，到点由 AlarmManager 发请求，App 关闭也能执行。 */
object SellScheduler {
    private const val PREFS = "dasha_sell_schedule"
    private const val KEY = "items"

    fun schedule(context: Context, deviceCode: String, deviceName: String, ids: List<Long>, at: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val all = JSONObject(prefs.getString(KEY, "{}") ?: "{}")
        val item = JSONObject()
            .put("device", deviceCode)
            .put("name", deviceName)
            .put("at", at)
            .put("ids", JSONArray(ids))
        all.put(deviceCode, item)
        prefs.edit().putString(KEY, all.toString()).apply()

        val intent = Intent(context, SellAlarmReceiver::class.java)
            .putExtra("device", deviceCode)
        val pending = PendingIntent.getBroadcast(
            context,
            deviceCode.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun cancel(context: Context, deviceCode: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val all = JSONObject(prefs.getString(KEY, "{}") ?: "{}")
        all.remove(deviceCode)
        prefs.edit().putString(KEY, all.toString()).apply()
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context,
            deviceCode.hashCode(),
            Intent(context, SellAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pending)
    }

    fun get(context: Context, deviceCode: String): JSONObject? =
        runCatching {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            JSONObject(prefs.getString(KEY, "{}") ?: "{}").optJSONObject(deviceCode)
        }.getOrNull()

    fun all(context: Context): List<JSONObject> = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val o = JSONObject(prefs.getString(KEY, "{}") ?: "{}")
        o.keys().asSequence().mapNotNull { o.optJSONObject(it) }.toList()
    }.getOrDefault(emptyList())
}
