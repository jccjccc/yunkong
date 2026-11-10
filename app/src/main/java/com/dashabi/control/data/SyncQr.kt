package com.dashabi.control.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private val syncJson = Json { ignoreUnknownKeys = true }

private fun syncHttpClient(): OkHttpClient {
    val trust = object : javax.net.ssl.X509TrustManager {
        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
    }
    val context = javax.net.ssl.SSLContext.getInstance("TLS")
    context.init(null, arrayOf(trust), java.security.SecureRandom())
    return OkHttpClient.Builder()
        .sslSocketFactory(context.socketFactory, trust)
        .hostnameVerifier { _, _ -> true }
        .build()
}

fun decryptSync(payload: String, keyText: String): String {
    val key = Base64.getUrlDecoder().decode(keyText)
    val bytes = Base64.getUrlDecoder().decode(payload)
    val iv = bytes.copyOfRange(0, 12)
    val encrypted = bytes.copyOfRange(12, bytes.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
    return String(cipher.doFinal(encrypted), Charsets.UTF_8)
}

suspend fun downloadSyncPayload(code: String): String = withContext(Dispatchers.IO) {
    val parts = code.trim().split('|')
    require(parts.size == 3 && parts[0] == "DSYNC1") { "不是有效的同步二维码" }
    val id = parts[1]
    val key = parts[2]
    val client = syncHttpClient()
    val request = Request.Builder()
        .url("https://dpaste.com/$id.txt")
        .header("User-Agent", "DashaControl/1.0 (Android)")
        .build()
    client.newCall(request).execute().use { response ->
        val payload = response.body?.string()?.trim().orEmpty()
        if (!response.isSuccessful || payload.isBlank()) error("读取同步数据失败：HTTP ${response.code}")
        decryptSync(payload, key)
    }
}

fun applySyncPayload(
    json: String,
    onStamina: (String, Int?, Int?, Int?) -> Unit,
    onCredit: (String, Int) -> Unit
): Pair<Int, Int> {
    val root = syncJson.parseToJsonElement(json).jsonObject
    var levels = 0
    var credits = 0
    root["stamina"]?.jsonObject?.forEach { (code, value) ->
        val obj = value.jsonObject
        val t = obj["t"]?.jsonPrimitive?.content?.toIntOrNull()
        val r = obj["r"]?.jsonPrimitive?.content?.toIntOrNull()
        val aw = obj["aw"]?.jsonPrimitive?.content?.toIntOrNull()
        if (t != null || r != null || aw != null) {
            onStamina(code, t, r, aw)
            levels++
        }
    }
    root["credit"]?.jsonObject?.forEach { (code, value) ->
        val score = value.jsonObject["c"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@forEach
        if (score > 0) {
            onCredit(code, score)
            credits++
        }
    }
    return levels to credits
}
