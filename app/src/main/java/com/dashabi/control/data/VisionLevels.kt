package com.dashabi.control.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * 从特勤处截图识别出的两个等级：
 * training = 训练中心（体力），range = 靶场（负重）。
 */
data class VisionLevels(
    val training: Int?,
    val range: Int?
)

private val visionJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * 解析 DeepSeek 返回的内容。兼容：
 *  - {"training_center":6,"shooting_range":4}
 *  - ```json ... ``` 包裹
 *  - "训练中心：Lv.6，靶场：Lv.4" 这类纯文本
 *  - 字符串形式 "Lv.6"
 */
fun parseVisionLevels(content: String?): VisionLevels? {
    if (content.isNullOrBlank()) return null

    var training: Int? = null
    var range: Int? = null

    extractJsonObject(content)?.let { text ->
        val obj = runCatching { visionJson.parseToJsonElement(text) as? JsonObject }.getOrNull()
        if (obj != null) {
            training = levelFrom(obj, "training_center", "training", "trainingCenter", "训练中心")
            range = levelFrom(obj, "shooting_range", "range", "shootingRange", "靶场")
        }
    }

    if (training == null) {
        training = Regex("训练中心[^0-9]{0,12}(\\d+)").find(content)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
    if (range == null) {
        range = Regex("靶场[^0-9]{0,12}(\\d+)").find(content)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    if (training == null && range == null) return null
    return VisionLevels(training, range)
}

private fun extractJsonObject(content: String): String? {
    val start = content.indexOf('{')
    val end = content.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    return content.substring(start, end + 1)
}

private fun levelFrom(obj: JsonObject, vararg keys: String): Int? {
    for (key in keys) {
        val raw = when (val value = obj[key]) {
            is JsonPrimitive -> value.contentOrNullSafe()
            is JsonObject -> (value["level"] as? JsonPrimitive)?.contentOrNullSafe()
            else -> null
        } ?: continue
        Regex("\\d+").find(raw)?.value?.toIntOrNull()?.let { return it }
    }
    return null
}

private fun JsonPrimitive.contentOrNullSafe(): String? =
    if (this is kotlinx.serialization.json.JsonNull) null else content
/**
 * 解析仓库截图识别出的 .338AP（AW）子弹数量。兼容：
 *  - {"aw_bullets":123} / {"aw":123} / {"bullets":123}
 *  - ```json ... ``` 包裹
 *  - "AW子弹：123发" 这类纯文本
 */
fun parseVisionBullets(content: String?): Int? {
    if (content.isNullOrBlank()) return null

    extractJsonObject(content)?.let { text ->
        val obj = runCatching { visionJson.parseToJsonElement(text) as? JsonObject }.getOrNull()
        if (obj != null) {
            // 优先使用逐格列出的 stacks：只汇总名称里带 338 的弹药，避免把背包负重等数字算进来
            val stacks = obj["stacks"] as? JsonArray
            if (stacks != null) {
                var total = 0
                var found = false
                var matched = false
                stacks.forEach { element ->
                    val stack = element as? JsonObject ?: return@forEach
                    val name = (stack["name"] as? JsonPrimitive)?.contentOrNullSafe().orEmpty()
                    if (!name.contains("338")) return@forEach
                    found = true
                    val count = (stack["count"] as? JsonPrimitive)?.contentOrNullSafe()
                        ?.let { intFrom(it) } ?: return@forEach
                    total += count
                    matched = true
                }
                // 有 338 AP 格子但数量看不清时返回 null（记为识别失败），不要当成 0
                return when {
                    matched -> total
                    found -> null
                    else -> 0
                }
            }

            for (key in listOf("aw_bullets", "aw", "bullets", "bullet_count", "338ap")) {
                val raw = (obj[key] as? JsonPrimitive)?.contentOrNullSafe() ?: continue
                intFrom(raw)?.let { return it }
            }
        }
    }

    var bullets: Int? = Regex(
        "(?:AW|\\.338\\s*AP|338AP)[^0-9]{0,12}([\\d,，]+)",
        RegexOption.IGNORE_CASE
    ).find(content)?.groupValues?.getOrNull(1)?.let { intFrom(it) }

    if (bullets == null) {
        bullets = Regex("([\\d,，]+)\\s*发").find(content)?.groupValues?.getOrNull(1)?.let { intFrom(it) }
    }
    return bullets
}

private fun intFrom(raw: String): Int? {
    val digits = Regex("\\d[\\d,，]*").find(raw)?.value ?: return null
    return digits.replace(",", "").replace("，", "").toIntOrNull()
}