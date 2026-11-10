package com.dashabi.control.data

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** 手机端按设备码把体力/负重/AW/信用分同步到腾讯云 COS。 */
data class AssistantStatus(
    val version: String?,
    val at: Long,
    val msg: String? = null,
    val accessibility: Boolean? = null
)

class CloudSync(
    private val context: Context,
    private val stamina: StaminaStore,
    private val credit: CreditStore,
    private val gameNames: GameNameStore? = null
) {
    private val prefs = context.getSharedPreferences("dasha_cloud_sync", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val BACKEND = "https://particular-cheetah-d3cd845ac7552.service.tcloudbase.com/api"

    val deviceCode: String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.lowercase() ?: "android"

    val peerBind: String
        get() = prefs.getString("peer_bind", "").orEmpty()

    private val NL = "\n"

    var peerCode: String
        get() = prefs.getString("peer", "").orEmpty()
        set(value) { prefs.edit().putString("peer", value.trim()).apply() }

    private fun controlToken(): String =
        context.getSharedPreferences("dasha_control_settings", Context.MODE_PRIVATE)
            .getString("control_token", "").orEmpty()

    private fun signedUrl(method: String, key: String): String? = runCatching {
        val token = controlToken()
        if (token.isBlank()) return@runCatching null
        val body = org.json.JSONObject()
            .put("action", "sign")
            .put("control_token", token)
            .put("key", key)
            .put("method", method)
        val conn = java.net.URL(BACKEND).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = conn.responseCode
        val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
        conn.disconnect()
        if (code !in 200..299) null else org.json.JSONObject(text).optString("url").takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun open(method: String, key: String): HttpURLConnection {
        val url = signedUrl(method, key) ?: error("请先在设置里填写安全令牌")
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        return conn
    }

    private fun build(): String {
        val root = buildJsonObject {
            put("device", deviceCode)
            put("updatedAt", System.currentTimeMillis())
            putJsonObject("stamina") {
                stamina.state.value.forEach { (code, level) ->
                    putJsonObject(code) {
                        level.training?.let { put("t", it) }
                        level.range?.let { put("r", it) }
                        level.awBullets?.let { put("aw", it) }
                        put("ts", level.updatedAt)
                    }
                }
            }
            putJsonObject("rebootBind") {
                val binds = com.dashabi.control.remote.RebootBindStore.toJson(context)
                binds.keys().forEach { device ->
                    val o = binds.optJSONObject(device) ?: return@forEach
                    putJsonObject(device) {
                        put("code", o.optString("code"))
                        put("ts", o.optLong("ts", 0L))
                    }
                }
            }
            putJsonObject("credit") {
                credit.state.value.forEach { (uin, entry) ->
                    putJsonObject(uin) {
                        put("c", entry.score)
                        put("ts", entry.updatedAt)
                    }
                }
            }
            gameNames?.let { store ->
                putJsonObject("gameNames") {
                    store.state.value.forEach { (code, entry) ->
                        putJsonObject(code) {
                            put("n", entry.name)
                            put("ts", entry.ts)
                        }
                    }
                }
            }
        }
        return root.toString()
    }

    /** 通过 CloudBase 后端下发指令；返回后端指令 id。 */
    private suspend fun backendSend(target: String, cmd: String, payload: org.json.JSONObject = org.json.JSONObject()): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = org.json.JSONObject()
                    .put("action", "send")
                    .put("control_token", controlToken())
                    .put("device_code", target)
                    .put("app_device_code", AppLicense.deviceCode(context))
                    .put("license_token", AppLicense.get(context).orEmpty())
                    .put("cmd", cmd)
                    .put("payload", payload)
                val conn = java.net.URL(BACKEND).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
                conn.disconnect()
                if (code !in 200..299) null
                else org.json.JSONObject(text).optString("id").takeIf { it.isNotBlank() }
            }.getOrNull()
        }

    suspend fun sendReboot(target: String): Boolean = backendSend(target, "reboot") != null

    suspend fun sendUpdate(target: String): Boolean = backendSend(target, "update") != null

    suspend fun sendBrightness(target: String, percent: Int): Boolean =
        backendSend(target, "brightness", org.json.JSONObject().put("percent", percent)) != null

    suspend fun sendTimAccounts(target: String, all: List<String>): Boolean =
        backendSend(
            target,
            "tim_accounts",
            org.json.JSONObject().put("all", org.json.JSONArray(all))
        ) != null

    /** 批量上传哈夫币快照（5 分钟一个点，服务器忽略重复）。 */
    suspend fun recordHaf(points: List<Triple<String, Long, Double>>): Boolean = withContext(Dispatchers.IO) {
        if (points.isEmpty()) return@withContext true
        runCatching {
            val arr = org.json.JSONArray()
            points.forEach { (account, ts, value) ->
                arr.put(org.json.JSONObject().put("account_code", account).put("ts", ts).put("value", value))
            }
            val body = org.json.JSONObject()
                .put("action", "record_haf")
                .put("control_token", controlToken())
                .put("app_device_code", AppLicense.deviceCode(context))
                .put("license_token", AppLicense.get(context).orEmpty())
                .put("snapshots", arr)
            val conn = java.net.URL(BACKEND).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            conn.inputStream?.close()
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)
    }

    suspend fun getAnnotations(): Map<String, String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = org.json.JSONObject()
                .put("action", "get_annotations")
                .put("control_token", controlToken())
            val conn = java.net.URL(BACKEND).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 15000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
            conn.disconnect()
            if (code !in 200..299) return@runCatching emptyMap()
            val rows = org.json.JSONObject(text).optJSONArray("rows") ?: return@runCatching emptyMap()
            (0 until rows.length()).mapNotNull { i ->
                val o = rows.optJSONObject(i) ?: return@mapNotNull null
                val c = o.optString("account_code")
                val n = o.optString("note")
                if (c.isBlank() || n.isBlank()) null else c to n
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    suspend fun deleteAnnotation(accountCode: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val body = org.json.JSONObject()
                .put("action", "delete_annotation")
                .put("control_token", controlToken())
                .put("account_code", accountCode)
            val conn = java.net.URL(BACKEND).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            conn.inputStream?.close()
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)
    }

    suspend fun setAnnotation(accountCode: String, note: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val body = org.json.JSONObject()
                .put("action", "set_annotation")
                .put("control_token", controlToken())
                .put("account_code", accountCode)
                .put("note", note.take(500))
            val conn = java.net.URL(BACKEND).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            conn.inputStream?.close()
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)
    }

    suspend fun hafMaxSince(since: Long, until: Long = 0L): Map<String, Double> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = org.json.JSONObject()
                    .put("action", "haf_max")
                    .put("control_token", controlToken())
                    .put("app_device_code", AppLicense.deviceCode(context))
                    .put("license_token", AppLicense.get(context).orEmpty())
                    .put("since", since)
                    .put("until", until)
                val conn = java.net.URL(BACKEND).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 8000
                conn.readTimeout = 20000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
                conn.disconnect()
                if (code !in 200..299) return@runCatching emptyMap()
                val obj = org.json.JSONObject(text).optJSONObject("max") ?: return@runCatching emptyMap()
                obj.keys().asSequence().mapNotNull { k ->
                    val v = obj.optDouble(k, Double.NaN)
                    if (v.isNaN()) null else k to v
                }.toMap()
            }.getOrDefault(emptyMap())
        }

    suspend fun hafHistory(accountCode: String, since: Long): List<Pair<Long, Double>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = org.json.JSONObject()
                    .put("action", "haf_history")
                    .put("control_token", controlToken())
                    .put("app_device_code", AppLicense.deviceCode(context))
                    .put("license_token", AppLicense.get(context).orEmpty())
                    .put("account_code", accountCode)
                    .put("since", since)
                val conn = java.net.URL(BACKEND).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 8000
                conn.readTimeout = 15000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val text = if (code in 200..299) conn.inputStream.bufferedReader().readText() else ""
                conn.disconnect()
                if (code !in 200..299) return@runCatching emptyList()
                val rows = org.json.JSONObject(text).optJSONArray("points") ?: return@runCatching emptyList()
                (0 until rows.length()).mapNotNull { i ->
                    val o = rows.optJSONObject(i) ?: return@mapNotNull null
                    o.optLong("ts") to o.optDouble("value")
                }
            }.getOrDefault(emptyList())
        }

    suspend fun scheduleSell(
        assistantCode: String,
        deviceCode: String,
        ids: List<Long>,
        at: Long
    ): Boolean = withContext(Dispatchers.IO) {        runCatching {
            val body = org.json.JSONObject()
                .put("action", "schedule_sell")
                .put("control_token", controlToken())
                .put("app_device_code", AppLicense.deviceCode(context))
                .put("license_token", AppLicense.get(context).orEmpty())
                .put("assistant_code", assistantCode)
                .put("device_code", deviceCode)
                .put("ids", org.json.JSONArray(ids))
                .put("at", at)
            val conn = java.net.URL(BACKEND).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            conn.inputStream?.close()
            conn.disconnect()
            code in 200..299
        }.getOrDefault(false)
    }

    /** 助手心跳文件里的版本。 */
    suspend fun assistantStatus(assistantCode: String): AssistantStatus? = withContext(Dispatchers.IO) {
        val o = getJson("status/" + assistantCode + ".json") ?: return@withContext null
        AssistantStatus(
            version = o.optString("version").takeIf { it.isNotBlank() },
            at = o.optLong("at", 0L),
            msg = o.optString("msg").takeIf { it.isNotBlank() },
            accessibility = if (o.has("acc")) o.optBoolean("acc") else null
        )
    }

    /** 云端最新助手版本。 */
    suspend fun assistantLatestVersion(): String? = withContext(Dispatchers.IO) {
        getJson("latest-reboot.json")?.optString("version")?.takeIf { it.isNotBlank() }
    }

    private fun getJson(key: String): org.json.JSONObject? = runCatching {
        val conn = open("GET", key)
        val text = if (conn.responseCode in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else null
        conn.disconnect()
        text?.let { org.json.JSONObject(it) }
    }.getOrNull()

    /** 上传二维码图片，返回公网地址。 */
    suspend fun uploadQr(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching {
            val key = "qr/" + java.util.UUID.randomUUID().toString().replace("-", "") + ".png"
            val conn = open("PUT", key)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "image/png")
            conn.outputStream.use { it.write(bytes) }
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            if (ok) signedUrl("GET", key) else null
        }.getOrNull()
    }

    /** 给指定云控助手下发扫码登录指令。 */
    suspend fun sendQrLogin(target: String, qq: String, url: String): String? =
        backendSend(target, "qrlogin", org.json.JSONObject().put("qq", qq).put("url", url))

    /** 读云控助手回报的扫码结果。 */
    suspend fun qrStatus(target: String, cmdId: String = "", since: Long = 0L): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = open("GET", "status/" + target + ".json")
            val text = if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else ""
            conn.disconnect()
            if (text.isBlank()) null else {
                val o = org.json.JSONObject(text)
                if (cmdId.isNotBlank() && o.optString("id") != cmdId) return@withContext null
                if (cmdId.isBlank() && o.optLong("at", 0L) < since) return@withContext null
                val prefix = when {
                    o.isNull("ok") -> "⏳ "
                    o.optBoolean("ok") -> "✅ "
                    else -> "❌ "
                }
                prefix + o.optString("msg")
            }
        }.getOrNull()
    }

    /** 让云控助手中途停止扫码流程（走后端 MQTT，秒到）。 */
    suspend fun cancelQr(target: String): Boolean = backendSend(target, "cancelqr") != null

    suspend fun sync(): String = withContext(Dispatchers.IO) {
        runCatching {
            if (stamina.state.value.isNotEmpty() || credit.state.value.isNotEmpty() ||
                gameNames?.state?.value?.isNotEmpty() == true ||
                com.dashabi.control.remote.RebootBindStore.toJson(context).length() > 0
            ) {
                val conn = open("PUT", "$deviceCode.json")
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(build().toByteArray()) }
                val ok = conn.responseCode in 200..299
                conn.disconnect()
                if (!ok) error("上传失败")
            }
            val peer = peerCode.trim()
            if (peer.isBlank()) return@runCatching "本机已上传，填写对方设备码后拉取"
            val get = open("GET", "$peer.json")
            val text = if (get.responseCode in 200..299) {
                get.inputStream.bufferedReader().readText()
            } else null
            get.disconnect()
            if (text.isNullOrBlank()) return@runCatching "对方还没有上传数据"
            val root = json.parseToJsonElement(text).jsonObject
            var n = 0
            mergeRebootBind(root)
            root["stamina"]?.jsonObject?.forEach { (code, value) ->
                val o = value.jsonObject
                val ts = o["ts"]?.jsonPrimitive?.longOrNull ?: 0L
                val local = stamina.get(code)
                if (local == null || ts > local.updatedAt) {
                    stamina.put(
                        code,
                        o["t"]?.jsonPrimitive?.intOrNull,
                        o["r"]?.jsonPrimitive?.intOrNull,
                        o["aw"]?.jsonPrimitive?.intOrNull
                    )
                    n++
                }
            }
            root["credit"]?.jsonObject?.forEach { (uin, value) ->
                val o = value.jsonObject
                val score = o["c"]?.jsonPrimitive?.intOrNull ?: return@forEach
                val ts = o["ts"]?.jsonPrimitive?.longOrNull ?: 0L
                val local = credit.get(uin)
                if (local == null || ts > local.updatedAt) {
                    val previous = when {
                        local == null -> null
                        local.score != score -> local.score
                        else -> local.previousScore
                    }
                    credit.put(uin, (local ?: CreditEntry(score)).copy(score = score, updatedAt = ts, previousScore = previous))
                    n++
                }
            }
            root["gameNames"]?.jsonObject?.forEach { (code, value) ->
                val o = value.jsonObject
                val name = o["n"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val ts = o["ts"]?.jsonPrimitive?.longOrNull ?: 0L
                val store = gameNames ?: return@forEach
                val local = store.state.value[code]
                if (name.isNotEmpty() && (local == null || ts > local.ts)) {
                    store.put(code, name, ts)
                    n++
                }
            }
            "同步完成，更新 $n 项"
        }.getOrElse { "同步失败：${it.message}" }
    }


    /** 电脑端下发来的绑定关系，按设备码 + 时间戳合并，逻辑同体力/信用分。 */
    private fun mergeRebootBind(root: kotlinx.serialization.json.JsonObject) {
        val section = root["rebootBind"]?.jsonObject ?: return
        val storedRaw = prefs.getString("peer_bind_raw", null)
        val local = LinkedHashMap<String, Pair<String, Long>>()
        if (!storedRaw.isNullOrBlank()) {
            runCatching {
                val obj = json.parseToJsonElement(storedRaw).jsonObject
                obj.forEach { (k, v) ->
                    val o = v.jsonObject
                    local[k] = (o["code"]?.jsonPrimitive?.contentOrNull.orEmpty()) to
                        (o["ts"]?.jsonPrimitive?.longOrNull ?: 0L)
                }
            }
        }
        section.forEach { (device, v) ->
            val o = runCatching { v.jsonObject }.getOrNull() ?: return@forEach
            val code = o["code"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val ts = o["ts"]?.jsonPrimitive?.longOrNull ?: 0L
            val old = local[device]
            if (code.isNotEmpty() && (old == null || ts >= old.second)) local[device] = code to ts
        }
        val out = org.json.JSONObject()
        local.forEach { (k, v) ->
            out.put(k, org.json.JSONObject().put("code", v.first).put("ts", v.second))
        }
        prefs.edit().putString("peer_bind_raw", out.toString()).apply()
        // 写进设备详情页真正读的那张表
        com.dashabi.control.remote.RebootBindStore.merge(context, out)
        val readable = local.entries.joinToString(NL) { (device, v) ->
            device.takeLast(8) + " → " + v.first
        }
        prefs.edit().putString("peer_bind", readable).apply()
    }

}
