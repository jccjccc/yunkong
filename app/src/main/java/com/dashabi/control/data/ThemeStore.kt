package com.dashabi.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    DARK("暗色"),
    LIGHT("亮色")
}

/** 主题选择（跟随系统 / 暗色 / 亮色），持久化在本机。 */
class ThemeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("dasha_theme", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }

    private fun read(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString(KEY_MODE, null).orEmpty())
    }.getOrDefault(ThemeMode.LIGHT)

    companion object {
        private const val KEY_MODE = "mode"
    }
}
