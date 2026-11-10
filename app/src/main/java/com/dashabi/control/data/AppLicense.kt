package com.dashabi.control.data

import android.content.Context
import android.provider.Settings
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 短时服务器授权令牌：绑定本机设备码，10 分钟有效。 */
object AppLicense {
    private const val BACKEND = "https://particular-cheetah-d3cd845ac7552.service.tcloudbase.com/api"
    @Volatile private var token: String? = null
    @Volatile private var expiresAt: Long = 0

    fun deviceCode(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.lowercase() ?: "android"

    @Synchronized
    fun get(context: Context, force: Boolean = false): String? {
        if (!force && token != null && System.currentTimeMillis() < expiresAt - 60_000) return token
        return runCatching {
            val body = JSONObject()
                .put("action", "license")
                .put("device_code", deviceCode(context))
            val conn = URL(BACKEND).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
            conn.disconnect()
            if (code !in 200..299) return@runCatching null
            val o = JSONObject(text)
            token = o.optString("token").takeIf { it.isNotBlank() }
            expiresAt = o.optLong("expires_at", 0L)
            token
        }.getOrNull()
    }
}
