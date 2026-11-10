package com.dashabi.control.data

import android.content.Context
import android.provider.Settings
import java.net.HttpURLConnection
import java.net.URL

/** 15 分钟试用 + 白名单（whitelist/<设备码>.txt）。 */
class License(context: Context) {
    private val prefs = context.getSharedPreferences("dasha_license", Context.MODE_PRIVATE)

    val deviceCode: String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.lowercase() ?: "android"

    fun trialStart(): Long {
        val saved = prefs.getLong("start", 0L)
        if (saved > 0) return saved
        val now = System.currentTimeMillis()
        prefs.edit().putLong("start", now).apply()
        return now
    }

    fun whitelisted(): Boolean {
        val now = System.currentTimeMillis()
        val conn = URL("https://dasha-update-1313972671.cos.ap-shanghai.myqcloud.com/whitelist/$deviceCode.txt")
            .openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        return try {
            if (conn.responseCode in 200..299) {
                prefs.edit().putLong("whitelist_ok_at", now).apply()
                true
            } else {
                // 网络/服务异常时，7 天内成功过就先用缓存，避免误锁
                now - prefs.getLong("whitelist_ok_at", 0L) < 7 * 24 * 3600_000L
            }
        } catch (_: Exception) {
            now - prefs.getLong("whitelist_ok_at", 0L) < 7 * 24 * 3600_000L
        } finally {
            conn.disconnect()
        }
    }

    fun expired(): Boolean = System.currentTimeMillis() - trialStart() > 24 * 60 * 60 * 1000L
}
