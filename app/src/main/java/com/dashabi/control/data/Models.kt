package com.dashabi.control.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/* ------------------------------------------------------------------ */
/*  通用信封                                                            */
/* ------------------------------------------------------------------ */

@Serializable
data class Envelope<T>(
    val data: T? = null,
    val meta: Meta? = null
)

@Serializable
data class Meta(
    @SerialName("request_id") val requestId: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    val stale: Boolean = false,
    val pagination: Pagination? = null,
    @SerialName("device_code") val deviceCode: String? = null,
    @SerialName("failed_device_count") val failedDeviceCount: Int? = null
)

@Serializable
data class Pagination(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 1
)

@Serializable
data class ApiErrorBody(val error: ApiError? = null)

@Serializable
data class ApiError(
    val code: String? = null,
    val message: String? = null,
    @SerialName("request_id") val requestId: String? = null
)

data class Paged<T>(val items: List<T>, val meta: Meta?)

/* ------------------------------------------------------------------ */
/*  Key 身份                                                            */
/* ------------------------------------------------------------------ */

@Serializable
data class Me(
    val name: String? = null,
    val scopes: List<String> = emptyList()
)

/* ------------------------------------------------------------------ */
/*  设备                                                                */
/* ------------------------------------------------------------------ */

@Serializable
data class DeviceStatus(
    val code: String? = null,
    val running: Boolean = false,
    @SerialName("task_name") val taskName: String? = null,
    val error: String? = null,
    @SerialName("timer_enabled") val timerEnabled: Boolean? = null,
    val online: Boolean? = null,
    @SerialName("service_enabled") val serviceEnabled: Boolean? = null
)

@Serializable
data class Device(
    @SerialName("device_code") val deviceCode: String = "",
    val name: String? = null,
    val model: String? = null,
    val tag: String? = null,
    @SerialName("account_count") val accountCount: Int = 0,
    val status: DeviceStatus? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class DeviceLive(
    @SerialName("device_code") val deviceCode: String? = null,
    val status: DeviceStatus? = null
)

@Serializable
data class Summary(
    val devices: Int = 0,
    val accounts: Int = 0,
    val running: Int = 0,
    @SerialName("waiting_next_round") val waitingNextRound: Int = 0,
    val stopped: Int = 0,
    @SerialName("accessibility_offline") val accessibilityOffline: Int = 0,
    val unknown: Int = 0
)

/* ------------------------------------------------------------------ */
/*  账号                                                                */
/* ------------------------------------------------------------------ */

@Serializable
data class Facility(
    val level: Int? = null,
    @SerialName("finish_at") val finishAt: String? = null
)

@Serializable
data class Facilities(
    val tech: Facility? = null,
    val workbench: Facility? = null,
    val pharmacy: Facility? = null,
    val armory: Facility? = null
)

@Serializable
data class ImageInfo(
    val available: Boolean = false,
    @SerialName("captured_at") val capturedAt: String? = null,
    val url: String? = null
)

@Serializable
data class Images(
    val profile: ImageInfo? = null,
    val tqc: ImageInfo? = null,
    val store: ImageInfo? = null
)

@Serializable
data class Account(
    @SerialName("account_code") val accountCode: String = "",
    @SerialName("device_code") val deviceCode: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("login_type") val loginType: String? = null,
    @SerialName("game_id") val gameId: String? = null,
    @SerialName("game_nick") val gameNick: String? = null,
    @SerialName("upstream_remark") val upstreamRemark: String? = null,
    @SerialName("website_note") val websiteNote: String? = null,
    val status: String? = null,
    @SerialName("raw_status") val rawStatus: String? = null,
    val paused: Boolean = false,
    @SerialName("action_level") val actionLevel: String? = null,
    @SerialName("total_asset") val totalAsset: String? = null,
    @SerialName("haf_coin") val hafCoin: String? = null,
    @SerialName("asset_updated_at") val assetUpdatedAt: String? = null,
    val facilities: Facilities? = null,
    val images: Images? = null
)

/* ------------------------------------------------------------------ */
/*  控制返回（结构未公开，统一按 JsonElement 处理）                        */
/* ------------------------------------------------------------------ */

typealias RawJson = JsonElement
