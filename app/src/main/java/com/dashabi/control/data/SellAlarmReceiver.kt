package com.dashabi.control.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SellAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SellScheduler.all(context).forEach { item ->
                val code = item.optString("device")
                val at = item.optLong("at", 0L)
                val ids = item.optJSONArray("ids") ?: return@forEach
                val list = (0 until ids.length()).map { ids.optLong(it) }
                if (code.isNotBlank() && at > 0) {
                    SellScheduler.schedule(context, code, item.optString("name"), list, at)
                }
            }
            return
        }
        val device = intent.getStringExtra("device").orEmpty()
        if (device.isBlank()) return
        val pending = goAsync()
        Thread {
            try {
                val item = SellScheduler.get(context, device) ?: return@Thread
                val idsArr = item.optJSONArray("ids") ?: return@Thread
                val ids = (0 until idsArr.length()).map { idsArr.optLong(it) }
                if (ids.isEmpty()) return@Thread
                val settings = SettingsStore(context).current()
                val url = URL(settings.baseUrl.trimEnd('/') + "/devices/" + device + "/sell")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer " + settings.apiKey)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(JSONObject().put("ids", idsArr).toString().toByteArray()) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
            } finally {
                SellScheduler.cancel(context, device)
                pending.finish()
            }
        }.start()
    }
}
