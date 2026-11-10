package com.dashabi.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CreditEntry(
    val score: Int,
    val nickname: String? = null,
    val cookies: String? = null,
    val updatedAt: Long = 0,
    val previousScore: Int? = null
)

class CreditStore(context: Context) {
    private val prefs = context.getSharedPreferences("qq_credit", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _state = MutableStateFlow(load())
    val state: StateFlow<Map<String, CreditEntry>> = _state

    fun get(accountCode: String): CreditEntry? = _state.value[accountCode]

    fun put(accountCode: String, entry: CreditEntry) {
        val old = _state.value[accountCode]
        val previous = when {
            old == null -> entry.previousScore
            old.score != entry.score -> old.score
            else -> entry.previousScore ?: old.previousScore
        }
        val next = _state.value + (accountCode to entry.copy(previousScore = previous))
        _state.value = next
        prefs.edit().putString(KEY, json.encodeToString(next)).apply()
    }

    fun remove(accountCode: String) {
        val next = _state.value - accountCode
        _state.value = next
        prefs.edit().putString(KEY, json.encodeToString(next)).apply()
    }

    private fun load(): Map<String, CreditEntry> = runCatching {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        json.decodeFromString<Map<String, CreditEntry>>(raw)
    }.getOrDefault(emptyMap())

    private companion object {
        const val KEY = "entries"
    }
}
