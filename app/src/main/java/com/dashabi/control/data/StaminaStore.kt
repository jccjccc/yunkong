package com.dashabi.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * AI 识别结果：
 * training = 训练中心（体力），range = 靶场（负重），awBullets = 仓库 .338AP（AW）子弹数量。
 */
data class StaminaLevels(
    val training: Int? = null,
    val range: Int? = null,
    val awBullets: Int? = null,
    val updatedAt: Long = 0L,
    /** 识别 AW 所用的提示词版本，升级后旧数据会自动重识别一次。 */
    val awVersion: Int = 0
)

class StaminaStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("dasha_stamina_levels", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Map<String, StaminaLevels>> = _state.asStateFlow()

    fun get(accountCode: String): StaminaLevels? = _state.value[accountCode]

    /**
     * 增量写入：只覆盖非空字段，这样「只识别未登记」时可以分别补齐等级或 AW 数量，
     * 不会因为某个图片识别失败而把已经登记的数据清空。
     */
    fun put(accountCode: String, training: Int?, range: Int?, awBullets: Int?) {
        if (accountCode.isBlank()) return
        if (training == null && range == null && awBullets == null) return
        val updated = _state.value.toMutableMap()
        val old = updated[accountCode]
        updated[accountCode] = StaminaLevels(
            training = training ?: old?.training,
            range = range ?: old?.range,
            awBullets = awBullets ?: old?.awBullets,
            updatedAt = System.currentTimeMillis(),
            awVersion = if (awBullets != null) AW_PROMPT_VERSION else (old?.awVersion ?: 0)
        )
        _state.value = updated
        persist(updated)
    }

    fun clear() {
        _state.value = emptyMap()
        prefs.edit().remove(KEY_LEVELS).apply()
    }

    private fun persist(levels: Map<String, StaminaLevels>) {
        val obj = buildJsonObject {
            levels.forEach { (code, value) ->
                putJsonObject(code) {
                    put("t", value.training)
                    put("r", value.range)
                    put("aw", value.awBullets)
                    put("awv", value.awVersion)
                    put("ts", value.updatedAt)
                }
            }
        }
        prefs.edit().putString(KEY_LEVELS, obj.toString()).apply()
    }

    private fun load(): Map<String, StaminaLevels> {
        val raw = prefs.getString(KEY_LEVELS, null) ?: return emptyMap()
        return runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            obj.mapNotNull { (code, element) ->
                val value = element as? JsonObject ?: return@mapNotNull null
                val training = (value["t"] as? JsonPrimitive)?.intOrNull
                val range = (value["r"] as? JsonPrimitive)?.intOrNull
                val aw = (value["aw"] as? JsonPrimitive)?.intOrNull
                val awVersion = (value["awv"] as? JsonPrimitive)?.intOrNull ?: 0
                val updated = (value["ts"] as? JsonPrimitive)?.longOrNull ?: 0L
                if (training == null && range == null && aw == null) null
                else code to StaminaLevels(training, range, aw, updated, awVersion)
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    companion object {
        /** 提示词/解析规则版本；变更后旧的 AW 结果会被视为需要重识别。 */
        const val AW_PROMPT_VERSION = 2

        private const val KEY_LEVELS = "levels"
    }
}
