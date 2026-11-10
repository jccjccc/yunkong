package com.dashabi.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** AI 从角色信息截图识别出的角色名，按账号存；和体力/AW 一样走 CloudSync 同步。 */
data class GameName(val name: String, val ts: Long = 0L)

class GameNameStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("dasha_game_names", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<Map<String, GameName>> = _state.asStateFlow()

    fun get(accountCode: String): String? = _state.value[accountCode]?.name

    fun put(accountCode: String, name: String, ts: Long = System.currentTimeMillis()) {
        if (accountCode.isBlank() || name.isBlank()) return
        val updated = _state.value.toMutableMap()
        updated[accountCode] = GameName(name.trim(), ts)
        _state.value = updated
        persist(updated)
    }

    private fun persist(values: Map<String, GameName>) {
        val obj = buildJsonObject {
            values.forEach { (code, value) ->
                putJsonObject(code) {
                    put("n", value.name)
                    put("ts", value.ts)
                }
            }
        }
        prefs.edit().putString(KEY, obj.toString()).apply()
    }

    private fun load(): Map<String, GameName> {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        return runCatching {
            json.parseToJsonElement(raw).jsonObject.mapNotNull { (code, element) ->
                val o = element as? JsonObject ?: return@mapNotNull null
                val name = (o["n"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
                val ts = (o["ts"] as? JsonPrimitive)?.longOrNull ?: 0L
                if (name.isBlank()) null else code to GameName(name, ts)
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    private companion object {
        const val KEY = "names"
    }
}
