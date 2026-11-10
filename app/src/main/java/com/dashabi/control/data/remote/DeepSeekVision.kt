package com.dashabi.control.data.remote

import android.util.Base64
import com.dashabi.control.data.AiSettings
import com.dashabi.control.data.VisionLevels
import com.dashabi.control.data.parseVisionBullets
import com.dashabi.control.data.parseVisionLevels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * DeepSeek 视觉模型（deepseek-flash）调用：
 *  - 特勤处截图 → 训练中心 / 靶场等级
 *  - 仓库截图 → .338AP（AW）子弹数量
 */
class DeepSeekVision {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(150, TimeUnit.SECONDS)
        .build()

    suspend fun recognize(jpeg: ByteArray, settings: AiSettings): VisionLevels? {
        val content = request(jpeg, settings, VISION_PROMPT)
        return parseVisionLevels(content) ?: throw IllegalStateException("模型未识别到等级")
    }

    suspend fun recognizeBullets(jpeg: ByteArray, settings: AiSettings): Int {
        val content = request(jpeg, settings, BULLETS_PROMPT)
        return parseVisionBullets(content) ?: throw IllegalStateException("模型未识别到 AW 子弹数量")
    }

    private suspend fun request(
        jpeg: ByteArray,
        settings: AiSettings,
        prompt: String
    ): String = withContext(Dispatchers.IO) {
        val base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
        val payload = buildVisionPayload(settings.model, base64, prompt)

        val request = Request.Builder()
            .url(settings.baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer ${settings.apiKey}")
            .header("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "DeepSeek 返回 HTTP ${response.code}：${body.take(180)}"
                )
            }
            extractContent(body) ?: throw IllegalStateException("DeepSeek 响应格式异常")
        }
    }

    private fun extractContent(body: String): String? {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return null
        val choices = root["choices"] as? JsonArray ?: return null
        val first = choices.firstOrNull() as? JsonObject ?: return null
        val message = first["message"] as? JsonObject ?: return null
        return (message["content"] as? JsonPrimitive)?.content
    }
}
