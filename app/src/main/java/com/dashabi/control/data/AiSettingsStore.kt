package com.dashabi.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiSettings(
    val apiKey: String,
    val baseUrl: String,
    val model: String
)

/**
 * DeepSeek 视觉识别配置（与平台 API Key 分开保存）。
 * 默认使用 DeepSeek 官方视觉模型 deepseek-flash。
 */
class AiSettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("dasha_ai_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AiSettings> = _state.asStateFlow()

    fun current(): AiSettings = _state.value

    fun save(apiKey: String, baseUrl: String, model: String) {
        val key = apiKey.trim()
        val url = baseUrl.trim().trimEnd('/').ifBlank { DEFAULT_BASE }
        val m = model.trim().ifBlank { DEFAULT_MODEL }
        prefs.edit()
            .putString(KEY_API, key)
            .putString(KEY_BASE, url)
            .putString(KEY_MODEL, m)
            .apply()
        _state.value = AiSettings(key, url, m)
    }

    private fun read(): AiSettings {
        val key = prefs.getString(KEY_API, null).orEmpty()
        val url = prefs.getString(KEY_BASE, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE
        val model = prefs.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        return AiSettings(key, url, model)
    }

    companion object {
        const val DEFAULT_BASE = "https://api.deepseek.com"
        const val DEFAULT_MODEL = "deepseek-flash"
        private const val KEY_API = "api_key"
        private const val KEY_BASE = "base_url"
        private const val KEY_MODEL = "model"
    }
}
