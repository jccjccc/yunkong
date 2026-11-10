package com.dashabi.control.data.remote

import com.dashabi.control.data.ApiResult
import com.dashabi.control.data.ApiErrorBody
import com.dashabi.control.data.Envelope
import com.dashabi.control.data.SettingsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

data class RawResponse(val body: String, val httpCode: Int)

/** 图片下载失败，带 HTTP 状态码与 Retry-After，便于识别任务重试与提示。 */
class ImageFetchException(
    val status: Int?,
    val retryAfterSeconds: Int?,
    message: String
) : Exception(message)

/**
 * 开放 API 客户端。
 *
 * - Base URL / API Key 来自 [SettingsStore]，修改后立即生效（拦截器每次请求读取）。
 * - 必须携带非空 User-Agent，否则 CDN 返回 403。
 * - 图片接口复用同一个 OkHttpClient，因此 Coil 加载图片时也自动带上鉴权头。
 */
class DashaApi(private val settingsProvider: SettingsProvider) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(80, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val s = settingsProvider.current()
            val builder = chain.request().newBuilder()
                .header("User-Agent", "DashaControl/1.0 (Android)")
            if (s.apiKey.isNotBlank()) {
                builder.header("Authorization", "Bearer ${s.apiKey}")
            }
            chain.proceed(builder.build())
        }
        .build()

    private fun settings() = settingsProvider.current()

    fun baseUrl(): String = settingsProvider.current().baseUrl.trimEnd('/')

    fun imageUrl(deviceCode: String, accountCode: String, kind: String): String =
        "${baseUrl()}/devices/$deviceCode/accounts/$accountCode/images/$kind"

    /** 把文档返回的相对地址（/index.php/...）变成可加载的绝对地址。 */
    fun absoluteUrl(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return pathOrUrl
        val base = baseUrl().toHttpUrlOrNull() ?: return null
        return base.resolve(pathOrUrl)?.toString()
    }

    /**
     * 下载图片原始字节（用于 AI 识别），自动带鉴权头。
     * 失败时抛出 [ImageFetchException]，带上 HTTP 状态码与 Retry-After，便于上层重试与提示。
     */
    suspend fun imageBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val parsed = url.toHttpUrlOrNull()
            ?: throw ImageFetchException(null, null, "图片地址无效")
        val request = Request.Builder().url(parsed).build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ImageFetchException(
                        status = response.code,
                        retryAfterSeconds = response.header("Retry-After")?.toIntOrNull(),
                        message = "HTTP ${response.code}"
                    )
                }
                response.body?.bytes() ?: throw ImageFetchException(response.code, null, "图片为空")
            }
        } catch (e: ImageFetchException) {
            throw e
        } catch (e: Throwable) {
            throw ImageFetchException(null, null, e.message ?: "网络错误")
        }
    }

    suspend fun call(
        method: String,
        path: String,
        query: Map<String, String> = emptyMap(),
        body: String? = null
    ): ApiResult<RawResponse> = withContext(Dispatchers.IO) {
        val url = try {
            (baseUrl() + path).toHttpUrlOrNull()
        } catch (e: Exception) {
            null
        } ?: return@withContext ApiResult.Err("Base URL 无效：${baseUrl()}")

        val finalUrl = url.newBuilder().apply {
            query.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()

        val requestBody = when {
            body != null -> body.toRequestBody(jsonMedia)
            method == "POST" || method == "PUT" || method == "PATCH" -> "{}".toRequestBody(jsonMedia)
            else -> null
        }

        val request = Request.Builder()
            .url(finalUrl)
            .method(method, requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    ApiResult.Ok(RawResponse(text, resp.code))
                } else {
                    ApiResult.Err(
                        message = errorMessage(text, resp.code),
                        code = errorCode(text),
                        http = resp.code,
                        retryAfterSec = resp.header("Retry-After")?.toIntOrNull(),
                        requestId = errorRequestId(text)
                    )
                }
            }
        } catch (e: UnknownHostException) {
            ApiResult.Err("无法解析服务器地址，请检查网络或设置中的 Base URL")
        } catch (e: SocketTimeoutException) {
            ApiResult.Err("请求超时，请检查网络后重试")
        } catch (e: SSLException) {
            ApiResult.Err("TLS 连接失败：${e.message}")
        } catch (e: IOException) {
            ApiResult.Err("网络错误：${e.message}")
        } catch (e: Throwable) {
            ApiResult.Err("请求失败：${e.message}")
        }
    }

    suspend fun <T> envelope(
        method: String,
        path: String,
        serializer: KSerializer<T>,
        query: Map<String, String> = emptyMap(),
        body: String? = null
    ): ApiResult<Envelope<T>> = when (val r = call(method, path, query, body)) {
        is ApiResult.Err -> r
        is ApiResult.Ok -> try {
            ApiResult.Ok(json.decodeFromString(Envelope.serializer(serializer), r.value.body))
        } catch (e: Exception) {
            ApiResult.Err("响应解析失败：${e.message}")
        }
    }

    /* ----------------------------- 错误解析 ----------------------------- */

    private fun errorObject(text: String) = runCatching {
        json.parseToJsonElement(text).jsonObject["error"]?.jsonObject
    }.getOrNull()

    private fun errorMessage(text: String, http: Int): String {
        val msg = (errorObject(text)?.get("message") as? JsonPrimitive)?.contentOrNull
        if (!msg.isNullOrBlank()) return msg
        return when (http) {
            401 -> "API Key 无效或已过期"
            403 -> "权限不足，或请求被 CDN 拦截"
            404 -> "资源不存在"
            409 -> "当前状态不允许该操作"
            422 -> "参数校验未通过"
            429 -> "请求过于频繁，请稍后再试"
            502, 503 -> "云控暂时不可用"
            else -> "HTTP $http"
        }
    }

    private fun errorCode(text: String): String? =
        (errorObject(text)?.get("code") as? JsonPrimitive)?.contentOrNull

    private fun errorRequestId(text: String): String? =
        (errorObject(text)?.get("request_id") as? JsonPrimitive)?.contentOrNull
}
