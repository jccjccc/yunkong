package com.dashabi.control.ui

import com.dashabi.control.data.Account
import com.dashabi.control.data.DeviceStatus
import com.dashabi.control.data.Facilities
import com.dashabi.control.data.Facility
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.Mint
import com.dashabi.control.ui.theme.Rose
import com.dashabi.control.ui.theme.Sky
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.Violet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 语义色，实际颜色由当前主题（暗色 / 亮色）决定。 */
enum class StatusTone { MINT, SKY, AMBER, ROSE, VIOLET, MUTED }

data class StatusStyle(val label: String, val tone: StatusTone) {
    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = when (tone) {
            StatusTone.MINT -> Mint
            StatusTone.SKY -> Sky
            StatusTone.AMBER -> Amber
            StatusTone.ROSE -> Rose
            StatusTone.VIOLET -> Violet
            StatusTone.MUTED -> TextMuted
        }
}

private val shortTime = DateTimeFormatter.ofPattern("MM-dd HH:mm")
private val fullTime = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
private val clockTime = DateTimeFormatter.ofPattern("HH:mm:ss")

fun deviceStatusStyle(code: String?): StatusStyle = when (code) {
    "running" -> StatusStyle("运行中", StatusTone.MINT)
    "waiting_next_round" -> StatusStyle("等待下一轮", StatusTone.SKY)
    "stopped" -> StatusStyle("已停止", StatusTone.ROSE)
    "accessibility_offline" -> StatusStyle("无障碍关闭", StatusTone.VIOLET)
    "unknown" -> StatusStyle("未知", StatusTone.MUTED)
    else -> StatusStyle(code?.takeIf { it.isNotBlank() } ?: "未知", StatusTone.MUTED)
}

fun deviceStatusStyle(status: DeviceStatus?): StatusStyle = deviceStatusStyle(status?.code)

val Account.statusKey: String
    get() = if (paused) "paused" else (status ?: "unknown")

fun accountStatusStyle(account: Account): StatusStyle = when (account.statusKey) {
    "normal" -> StatusStyle("正常", StatusTone.MINT)
    "paused" -> StatusStyle("已暂停", StatusTone.AMBER)
    "device_stopped" -> StatusStyle("设备停止", StatusTone.ROSE)
    "abnormal" -> StatusStyle("异常", StatusTone.ROSE)
    "frozen", "account_frozen" -> StatusStyle("账号冻结", StatusTone.ROSE)
    "unknown" -> StatusStyle("未知", StatusTone.MUTED)
    else -> StatusStyle(account.statusKey, StatusTone.MUTED)
}

fun accountStatusStyle(code: String): StatusStyle = when (code) {
    "normal" -> StatusStyle("正常", StatusTone.MINT)
    "paused" -> StatusStyle("已暂停", StatusTone.AMBER)
    "device_stopped" -> StatusStyle("设备停止", StatusTone.ROSE)
    "abnormal" -> StatusStyle("异常", StatusTone.ROSE)
    "frozen", "account_frozen" -> StatusStyle("账号冻结", StatusTone.ROSE)
    "unknown" -> StatusStyle("未知", StatusTone.MUTED)
    else -> StatusStyle(code, StatusTone.MUTED)
}

val AccountStatusFilters: List<Pair<String, String>> = listOf(
    "all" to "全部",
    "normal" to "正常",
    "paused" to "暂停",
    "abnormal" to "异常",
    "device_stopped" to "设备停止",
    "unknown" to "未知"
)

fun formatIso(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    return try {
        OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(shortTime)
    } catch (e: Exception) {
        try {
            LocalDateTime.parse(value.replace('T', ' ').substringBefore('.'))
                .format(shortTime)
        } catch (e2: Exception) {
            value
        }
    }
}

fun formatIsoFull(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    return try {
        OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(fullTime)
    } catch (e: Exception) {
        value
    }
}

fun clockOf(millis: Long?): String {
    if (millis == null) return "—"
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(clockTime)
}

fun loginTypeLabel(type: String?): String = when (type?.lowercase()) {
    "qq" -> "QQ"
    "wx", "wechat" -> "微信"
    "phone", "mobile" -> "手机"
    null, "" -> "未知"
    else -> type.uppercase()
}

fun scopeLabel(scope: String): String = when (scope) {
    "devices:read" -> "设备读取"
    "accounts:read" -> "账号读取"
    "images:read" -> "图片读取"
    "accounts:control" -> "账号控制"
    "devices:control" -> "设备控制"
    "schedules:write" -> "定时暂停"
    "notes:write" -> "备注修改"
    "sync:write" -> "主动同步"
    else -> scope
}

data class FacilityUi(val key: String, val label: String, val facility: Facility?)

fun facilityUiList(f: Facilities?): List<FacilityUi> = listOf(
    FacilityUi("tech", "技术", f?.tech),
    FacilityUi("workbench", "工作台", f?.workbench),
    FacilityUi("pharmacy", "制药", f?.pharmacy),
    FacilityUi("armory", "防具", f?.armory)
)

fun deviceTimerLabel(enabled: Boolean?): String = when (enabled) {
    true -> "已开启"
    false -> "已关闭"
    null -> "未知"
}

fun deviceOnlineLabel(online: Boolean?): String = when (online) {
    true -> "在线"
    false -> "离线"
    null -> "未知"
}

/** 已超时时长：天/小时/分钟。 */
fun formatOverdue(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(0L)
    val days = totalMinutes / 1440
    val hours = (totalMinutes % 1440) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> "${days}天${hours}小时"
        hours > 0 -> "${hours}小时${minutes}分"
        else -> "${minutes}分钟"
    }
}

/* ------------------------- 未公开结构的 JSON 展示 ------------------------- */

fun jsonKeyLabel(key: String): String = when (key) {
    "id" -> "ID"
    "active" -> "进行中"
    "status" -> "状态"
    "state" -> "状态"
    "message" -> "消息"
    "msg" -> "消息"
    "device_code" -> "设备"
    "account_code" -> "账号"
    "account_name" -> "账号名称"
    "start_at" -> "开始"
    "end_at" -> "结束"
    "created_at" -> "创建时间"
    "updated_at" -> "更新时间"
    "started_at" -> "开始时间"
    "finished_at" -> "结束时间"
    "ids" -> "账号 ID"
    "completed" -> "已完成"
    "completed_count" -> "完成数"
    "total" -> "总数"
    "locked" -> "锁定"
    "error" -> "错误"
    "type" -> "类型"
    "name" -> "名称"
    "code" -> "代码"
    else -> key
}

private fun primitiveText(p: JsonPrimitive): String =
    if (p is JsonNull) "" else p.content

fun flattenJson(element: JsonElement?, prefix: String = ""): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    when (element) {
        is JsonObject -> element.forEach { (k, v) ->
            val key = if (prefix.isBlank()) k else "$prefix.$k"
            when (v) {
                is JsonObject -> out += flattenJson(v, key)
                is JsonArray -> out += key to v.joinToString("、") { item ->
                    if (item is JsonPrimitive) primitiveText(item) else item.toString()
                }
                is JsonPrimitive -> out += key to primitiveText(v)
                else -> Unit
            }
        }
        is JsonArray -> element.forEachIndexed { i, v ->
            out += "#${i + 1}" to v.toString()
        }
        is JsonPrimitive -> if (prefix.isNotBlank()) out += prefix to primitiveText(element)
        else -> Unit
    }
    return out
}

fun jsonBoolean(element: JsonElement?, key: String): Boolean? {
    val obj = element as? JsonObject ?: return null
    return (obj[key] as? JsonPrimitive)?.booleanOrNull
}

fun jsonString(element: JsonElement?, key: String): String? {
    val obj = element as? JsonObject ?: return null
    return (obj[key] as? JsonPrimitive)?.contentOrNull
}

fun extractMessage(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val obj = runCatching {
        kotlinx.serialization.json.Json.parseToJsonElement(raw) as? JsonObject
    }.getOrNull() ?: return null
    val direct = (obj["message"] as? JsonPrimitive)?.contentOrNull
        ?: (obj["msg"] as? JsonPrimitive)?.contentOrNull
    if (!direct.isNullOrBlank()) return direct
    val data = obj["data"] as? JsonObject ?: return null
    return (data["message"] as? JsonPrimitive)?.contentOrNull
        ?: (data["msg"] as? JsonPrimitive)?.contentOrNull
        ?: (data["status"] as? JsonPrimitive)?.contentOrNull
        ?: (data["result"] as? JsonPrimitive)?.contentOrNull
}

fun actionSuccessText(raw: String?, fallback: String): String =
    extractMessage(raw)?.takeIf { it.isNotBlank() } ?: fallback

/**
 * 解析接口里的紧凑数值。
 *
 * 接口的写法并不统一：
 *  - haf_coin 以「千」为单位并带 K：17.050K 表示 17,050 千 = 17,050,000，
 *    其中的 "." 和 "," 都是千位分隔符（21,390K = 21,390 千 = 21,390,000）。
 *  - total_asset 用带一位小数的 M：31.5M = 31,500,000。
 *  - 无后缀时 "," 是千位分隔符（540,618 = 540,618）。
 * 无法解析时返回 null（排序时排在最后）。
 */
fun parseCompactNumber(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    var text = raw.trim()
        .replace(" ", "")
        .replace("\u00A0", "")
        .replace("，", ",")
        .replace("．", ".")
    if (text.isEmpty()) return null

    var multiplier = 1.0
    var hasK = false
    when (text.last().uppercaseChar()) {
        'K' -> {
            multiplier = 1_000.0
            hasK = true
            text = text.dropLast(1)
        }
        'M' -> {
            multiplier = 1_000_000.0
            text = text.dropLast(1)
        }
        'B' -> {
            multiplier = 1_000_000_000.0
            text = text.dropLast(1)
        }
        '万' -> {
            multiplier = 10_000.0
            text = text.dropLast(1)
        }
        '亿' -> {
            multiplier = 100_000_000.0
            text = text.dropLast(1)
        }
    }

    val dots = text.count { it == '.' }
    val commas = text.count { it == ',' }
    val separatorIndex = text.indexOfFirst { it == '.' || it == ',' }
    val digitsAfterSeparator = if (separatorIndex >= 0) text.length - separatorIndex - 1 else 0
    val singleSeparator = (dots == 1 && commas == 0) || (dots == 0 && commas == 1)

    val normalized = when {
        // K：前面的数字是「千」的数量，3 位一组的点/逗号是千位分隔符
        hasK && (dots + commas >= 2 || (singleSeparator && digitsAfterSeparator == 3)) ->
            text.replace(".", "").replace(",", "")
        hasK -> text.replace(',', '.')
        // 其他后缀或无后缀：同时出现时最后一个是小数点
        dots > 0 && commas > 0 -> {
            val decimalSeparator = if (text.lastIndexOf('.') > text.lastIndexOf(',')) '.' else ','
            val thousandSeparator = if (decimalSeparator == '.') ',' else '.'
            text.replace(thousandSeparator.toString(), "").replace(decimalSeparator, '.')
        }
        dots > 1 -> text.replace(".", "")
        commas > 1 -> text.replace(",", "")
        // 无后缀的单个逗号是千位分隔符；带 M/B 等后缀时按小数点
        commas == 1 && dots == 0 -> if (multiplier > 1.0) text.replace(',', '.') else text.replace(",", "")
        // 无后缀的单个点且后面正好 3 位，也是千位分隔符（接口把 863,506 写成 863.506）
        dots == 1 && commas == 0 && multiplier == 1.0 && digitsAfterSeparator == 3 ->
            text.replace(".", "")
        else -> text
    }

    val number = normalized.toDoubleOrNull() ?: return null
    return number * multiplier
}
/**
 * 哈夫币按「万」显示，单位 W：41,223K -> 4122.3W，540,618 -> 54.1W。
 * 直接基于真实数值（K = 千）换算，避免接口原始字符串的显示歧义。
 */
fun formatHafCoinWan(raw: String?): String {
    val value = parseCompactNumber(raw) ?: return "—"
    return formatWanAmount(value)
}

/** 把具体数值换算成「万」并加 W，最多保留一位小数（4122.3W / 54W）。 */
fun formatWanAmount(value: Double): String {
    if (value <= 0.0) return "—"
    val wan = value / 10_000.0
    val text = String.format(Locale.US, "%.1f", wan)
    return text.removeSuffix(".0") + "W"
}

/**
 * 账号搜索匹配：昵称、游戏 ID、账号（QQ 号）、备注、设备名、登录方式。
 * 本地过滤使用，输入即出结果，不再请求接口。
 */
fun Account.matchesSearch(raw: String): Boolean {
    val keyword = raw.trim().lowercase()
    if (keyword.isEmpty()) return true
    return listOfNotNull(
        gameNick,
        gameId,
        accountCode,
        upstreamRemark,
        websiteNote,
        deviceName,
        loginType
    ).any { it.lowercase().contains(keyword) }
}
/** 把毫秒时间戳格式化成 MM-dd HH:mm（用于展示 AI 识别时间）。 */
fun formatTimestamp(millis: Long): String {
    if (millis <= 0L) return "—"
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(shortTime)
}
/**
 * 自然排序：连续数字按数值比较，其余按字符比较。
 * 用于设备名，避免「设备10」排在「设备2」前面。
 */
fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            var endA = i
            while (endA < a.length && a[endA].isDigit()) endA++
            var endB = j
            while (endB < b.length && b[endB].isDigit()) endB++

            val numA = a.substring(i, endA).trimStart('0').ifEmpty { "0" }
            val numB = b.substring(j, endB).trimStart('0').ifEmpty { "0" }
            val byLength = numA.length.compareTo(numB.length)
            if (byLength != 0) return byLength
            val byValue = numA.compareTo(numB)
            if (byValue != 0) return byValue

            i = endA
            j = endB
        } else {
            val byChar = ca.lowercaseChar().compareTo(cb.lowercaseChar())
            if (byChar != 0) return byChar
            i++
            j++
        }
    }
    return (a.length - i).compareTo(b.length - j)
}
/**
 * 搜索相关度：备注命中优先级最高，其次是账号 / 游戏 ID、昵称、设备名。
 * 搜索时先按相关度降序，再按当前排序规则排序。
 */
fun Account.searchRelevance(raw: String): Int {
    val keyword = raw.trim().lowercase()
    if (keyword.isEmpty()) return 0

    fun score(value: String?, exact: Int, prefix: Int, contains: Int): Int {
        val text = value?.trim()?.lowercase() ?: return 0
        if (text.isEmpty()) return 0
        return when {
            text == keyword -> exact
            text.startsWith(keyword) -> prefix
            text.contains(keyword) -> contains
            else -> 0
        }
    }

    return maxOf(
        // 备注优先：备注命中排在账号 / 游戏 ID 之前
        score(upstreamRemark, 130, 124, 118),
        score(accountCode, 110, 100, 90),
        score(gameId, 106, 96, 86),
        score(gameNick, 56, 50, 44),
        score(deviceName, 30, 26, 22),
        score(websiteNote, 18, 16, 14),
        score(loginType, 12, 10, 8)
    )
}