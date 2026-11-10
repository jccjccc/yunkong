package com.dashabi.control.data

import com.dashabi.control.data.remote.DashaApi
import com.dashabi.control.data.remote.ImageFetchException
import kotlinx.coroutines.delay
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * 开放 API 的领域封装：把 JSON 信封拆成 UI 好用的类型，页码自动翻完。
 */
class Repository(private val api: DashaApi) {

    val json: Json get() = api.json

    fun imageUrl(deviceCode: String, accountCode: String, kind: String): String =
        api.imageUrl(deviceCode, accountCode, kind)

    fun absoluteUrl(pathOrUrl: String?): String? = api.absoluteUrl(pathOrUrl)

    /* ------------------------------ 身份 ------------------------------ */

    suspend fun me(): ApiResult<Me> =
        unwrap(api.envelope("GET", "/me", Me.serializer()))

    /* ------------------------------ 设备 ------------------------------ */

    suspend fun summary(): ApiResult<Summary> =
        unwrap(api.envelope("GET", "/status/summary", Summary.serializer()))

    suspend fun devicesPage(page: Int = 1, perPage: Int = 100): ApiResult<Paged<Device>> =
        when (val r = api.envelope(
            "GET", "/devices", ListSerializer(Device.serializer()),
            mapOf("page" to "$page", "per_page" to "$perPage")
        )) {
            is ApiResult.Err -> r
            is ApiResult.Ok -> ApiResult.Ok(Paged(r.value.data ?: emptyList(), r.value.meta))
        }

    suspend fun devices(): ApiResult<List<Device>> = collectAll { page -> devicesPage(page) }

    suspend fun device(deviceCode: String): ApiResult<Device> =
        unwrap(api.envelope("GET", "/devices/$deviceCode", Device.serializer()))

    suspend fun deviceLive(deviceCode: String): ApiResult<DeviceLive> =
        unwrap(api.envelope("GET", "/devices/$deviceCode/status", DeviceLive.serializer()))

    /* ------------------------------ 账号 ------------------------------ */

    suspend fun deviceAccountsPage(deviceCode: String, page: Int = 1, perPage: Int = 100): ApiResult<Paged<Account>> =
        when (val r = api.envelope(
            "GET", "/devices/$deviceCode/accounts", ListSerializer(Account.serializer()),
            mapOf("page" to "$page", "per_page" to "$perPage")
        )) {
            is ApiResult.Err -> r
            is ApiResult.Ok -> ApiResult.Ok(Paged(r.value.data ?: emptyList(), r.value.meta))
        }

    suspend fun deviceAccounts(deviceCode: String): ApiResult<List<Account>> =
        collectAll { page -> deviceAccountsPage(deviceCode, page) }

    suspend fun account(deviceCode: String, accountCode: String): ApiResult<Account> =
        unwrap(api.envelope("GET", "/devices/$deviceCode/accounts/$accountCode", Account.serializer()))

    suspend fun accountsPage(page: Int = 1, perPage: Int = 100, search: String = ""): ApiResult<Paged<Account>> {
        val query = buildMap {
            put("page", "$page")
            put("per_page", "$perPage")
            if (search.isNotBlank()) put("search", search)
        }
        return when (val r = api.envelope("GET", "/accounts", ListSerializer(Account.serializer()), query)) {
            is ApiResult.Err -> r
            is ApiResult.Ok -> ApiResult.Ok(Paged(r.value.data ?: emptyList(), r.value.meta))
        }
    }

    suspend fun accounts(search: String = ""): ApiResult<List<Account>> =
        collectAll { page -> accountsPage(page, search = search) }

    /**
     * 特勤处截图原始字节，供 AI 识别训练中心 / 靶场等级。
     * 图片接口限流 120 次/分钟，遇到 429/5xx 会按 Retry-After 或指数退避重试。
     */
    suspend fun tqcImageBytes(deviceCode: String, accountCode: String): ByteArray =
        imageBytesWithRetry(deviceCode, accountCode, "tqc")

    /** 仓库截图原始字节，供 AI 识别 .338AP（AW）子弹数量。 */
    suspend fun storeImageBytes(deviceCode: String, accountCode: String): ByteArray =
        imageBytesWithRetry(deviceCode, accountCode, "store")

    private suspend fun imageBytesWithRetry(
        deviceCode: String,
        accountCode: String,
        kind: String
    ): ByteArray {
        val url = api.imageUrl(deviceCode, accountCode, kind)
        var last: ImageFetchException? = null
        for (attempt in 0 until 4) {
            try {
                return api.imageBytes(url)
            } catch (e: ImageFetchException) {
                last = e
                val retryable = e.status == null || e.status == 429 || e.status >= 500
                if (!retryable || attempt == 3) throw e
                val waitMs = when {
                    e.retryAfterSeconds != null -> e.retryAfterSeconds * 1_000L
                    e.status == 429 -> 2_000L * (attempt + 1)
                    else -> 800L * (attempt + 1)
                }.coerceAtMost(10_000L)
                delay(waitMs)
            }
        }
        throw last ?: ImageFetchException(null, null, "图片下载失败")
    }

    suspend fun accountImages(deviceCode: String, accountCode: String): ApiResult<Images> =
        unwrap(api.envelope("GET", "/devices/$deviceCode/accounts/$accountCode/images", Images.serializer()))

    /* ------------------------------ 控制 ------------------------------ */

    suspend fun pause(deviceCode: String, accountCode: String): ApiResult<String> =
        action("POST", "/devices/$deviceCode/accounts/$accountCode/pause")

    suspend fun resume(deviceCode: String, accountCode: String): ApiResult<String> =
        action("POST", "/devices/$deviceCode/accounts/$accountCode/resume")

    suspend fun runProduction(deviceCode: String, accountCode: String): ApiResult<String> =
        action("POST", "/devices/$deviceCode/accounts/$accountCode/run-production")

    suspend fun timerEnable(deviceCode: String): ApiResult<String> =
        action("POST", "/devices/$deviceCode/timer/enable")

    suspend fun timerDisable(deviceCode: String): ApiResult<String> =
        action("POST", "/devices/$deviceCode/timer/disable")

    suspend fun stop(deviceCode: String, disableTimer: Boolean): ApiResult<String> =
        action(
            "POST", "/devices/$deviceCode/stop",
            buildJsonObject { put("disable_timer", disableTimer) }.toString()
        )

    suspend fun sync(deviceCode: String): ApiResult<String> =
        action("POST", "/devices/$deviceCode/sync")

    suspend fun setNote(deviceCode: String, accountCode: String, note: String): ApiResult<String> =
        action(
            "PUT", "/devices/$deviceCode/accounts/$accountCode/note",
            buildJsonObject { put("note", note) }.toString()
        )

    /* ------------------------------ 出售 ------------------------------ */

    suspend fun sellOne(deviceCode: String, accountCode: String): ApiResult<String> =
        action("POST", "/devices/$deviceCode/accounts/$accountCode/sell")

    suspend fun sellBatch(deviceCode: String, ids: List<Long>): ApiResult<String> =
        action(
            "POST", "/devices/$deviceCode/sell",
            buildJsonObject {
                putJsonArray("ids") { ids.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
            }.toString()
        )

    suspend fun sellStatus(deviceCode: String): ApiResult<JsonElement> =
        unwrap(api.envelope("GET", "/devices/$deviceCode/sell/status", JsonElement.serializer()))

    /* ---------------------------- 定时暂停 ---------------------------- */

    suspend fun schedules(deviceCode: String? = null, includeHistory: Boolean = true): ApiResult<List<JsonObject>> {
        val query = buildMap {
            if (!deviceCode.isNullOrBlank()) put("device_code", deviceCode)
            put("include_history", if (includeHistory) "true" else "false")
        }
        return when (val r = api.envelope("GET", "/pause-schedules", JsonElement.serializer(), query)) {
            is ApiResult.Err -> r
            is ApiResult.Ok -> {
                val data = r.value.data
                val list: List<JsonElement> = when (data) {
                    is JsonArray -> data.toList()
                    is JsonObject -> (data["items"] as? JsonArray)?.toList() ?: listOf(data)
                    else -> emptyList()
                }
                ApiResult.Ok(list.mapNotNull { it as? JsonObject })
            }
        }
    }

    suspend fun createSchedule(
        deviceCode: String,
        accountCode: String,
        accountName: String?,
        startAt: String,
        endAt: String
    ): ApiResult<String> = action(
        "POST", "/devices/$deviceCode/accounts/$accountCode/pause-schedules",
        buildJsonObject {
            if (!accountName.isNullOrBlank()) put("account_name", accountName)
            put("start_at", startAt)
            put("end_at", endAt)
        }.toString()
    )

    suspend fun cancelSchedule(id: Long): ApiResult<String> =
        action("DELETE", "/pause-schedules/$id")

    /* ------------------------------ 工具 ------------------------------ */

    private suspend fun action(method: String, path: String, body: String? = null): ApiResult<String> =
        when (val r = api.call(method, path, body = body)) {
            is ApiResult.Err -> r
            is ApiResult.Ok -> ApiResult.Ok(r.value.body)
        }

    private fun <T> unwrap(r: ApiResult<Envelope<T>>): ApiResult<T> = when (r) {
        is ApiResult.Err -> r
        is ApiResult.Ok -> r.value.data?.let { ApiResult.Ok(it) }
            ?: ApiResult.Err("响应中缺少 data 字段")
    }

    private suspend fun <T> collectAll(fetch: suspend (Int) -> ApiResult<Paged<T>>): ApiResult<List<T>> {
        val all = mutableListOf<T>()
        var page = 1
        var totalPages = 1
        while (page <= totalPages && page <= 100) {
            when (val r = fetch(page)) {
                is ApiResult.Err -> return r
                is ApiResult.Ok -> {
                    all += r.value.items
                    totalPages = r.value.meta?.pagination?.totalPages ?: 1
                    page++
                }
            }
        }
        return ApiResult.Ok(all)
    }
}
