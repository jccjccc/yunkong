package com.dashabi.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** 一次自动出售请求的结果。 */
data class AutoSellRecord(
    val lastAttemptAt: Long = 0L,
    val success: Boolean = false,
    val message: String = ""
)

/** 自动出售（哈夫币 < 100 万）的请求时间、结果与上次执行的整点。 */
class AutoSellStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("dasha_auto_sell", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _state = MutableStateFlow<Map<String, AutoSellRecord>>(emptyMap())
    val state: StateFlow<Map<String, AutoSellRecord>> = _state.asStateFlow()

    private var lastSlotValue: Long = 0L

    init {
        load()
    }

    fun get(accountCode: String): AutoSellRecord? = _state.value[accountCode]

    fun record(accountCode: String, success: Boolean, message: String, at: Long = System.currentTimeMillis()) {
        val updated = _state.value.toMutableMap()
        updated[accountCode] = AutoSellRecord(at, success, message)
        _state.value = updated
        persist()
    }

    fun lastSlot(): Long = lastSlotValue

    fun setLastSlot(slot: Long) {
        lastSlotValue = slot
        persist()
    }

    private fun persist() {
        val obj = buildJsonObject {
            put("lastSlot", lastSlotValue)
            putJsonObject("records") {
                _state.value.forEach { (code, record) ->
                    putJsonObject(code) {
                        put("ts", record.lastAttemptAt)
                        put("ok", record.success)
                        put("msg", record.message)
                    }
                }
            }
        }
        prefs.edit().putString(KEY, obj.toString()).apply()
    }

    private fun load() {
        val raw = prefs.getString(KEY, null) ?: return
        runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            lastSlotValue = (root["lastSlot"] as? JsonPrimitive)?.longOrNull ?: 0L
            val records = root["records"] as? JsonObject ?: return@runCatching
            _state.value = records.mapNotNull { (code, element) ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val at = (obj["ts"] as? JsonPrimitive)?.longOrNull ?: 0L
                val ok = (obj["ok"] as? JsonPrimitive)?.booleanOrNull ?: false
                val msg = (obj["msg"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                code to AutoSellRecord(at, ok, msg)
            }.toMap()
        }
    }

    companion object {
        private const val KEY = "auto_sell"
    }
}
