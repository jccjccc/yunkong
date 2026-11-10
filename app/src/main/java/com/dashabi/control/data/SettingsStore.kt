package com.dashabi.control.data

import android.content.Context
import com.dashabi.control.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val apiKey: String,
    val baseUrl: String
)

/** 抽象出设置来源，便于在 JVM 单元测试里直接注入 Key。 */
interface SettingsProvider {
    fun current(): AppSettings
}

/**
 * API Key 与 Base URL 的本地持久化。默认值来自 BuildConfig，
 * 用户可在「连接设置」里覆盖（例如换成自己另一个 Key）。
 */
class SettingsStore(context: Context) : SettingsProvider {

    private val prefs = context.applicationContext
        .getSharedPreferences("dasha_control_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    override fun current(): AppSettings = _state.value

    fun save(apiKey: String, baseUrl: String) {
        val key = apiKey.trim()
        val url = normalize(baseUrl)
        prefs.edit()
            .putString(KEY_API, key)
            .putString(KEY_BASE, url)
            .apply()
        _state.value = AppSettings(key, url)
    }

    fun resetToDefault() = save(BuildConfig.DEFAULT_API_KEY, BuildConfig.DEFAULT_BASE_URL)

    fun controlToken(): String = prefs.getString(KEY_CONTROL, "").orEmpty()

    fun saveControlToken(value: String) {
        prefs.edit().putString(KEY_CONTROL, value.trim()).apply()
    }

    fun isDefaultKey(): Boolean = current().apiKey == BuildConfig.DEFAULT_API_KEY

    private fun read(): AppSettings {
        val key = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.DEFAULT_API_KEY
        val url = prefs.getString(KEY_BASE, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.DEFAULT_BASE_URL
        return AppSettings(key, normalize(url))
    }

    private fun normalize(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        return trimmed.ifBlank { BuildConfig.DEFAULT_BASE_URL }
    }

    companion object {
        private const val KEY_API = "api_key"
        private const val KEY_BASE = "base_url"
        private const val KEY_CONTROL = "control_token"
    }
}
