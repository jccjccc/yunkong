package com.dashabi.control.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 界面偏好（目前只有账号总览的缩略模式），保存在本机。 */
class UiPrefsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("dasha_ui_prefs", Context.MODE_PRIVATE)

    private val _compactMode = MutableStateFlow(prefs.getBoolean(KEY_COMPACT, false))
    val compactMode: StateFlow<Boolean> = _compactMode.asStateFlow()

    fun setCompactMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COMPACT, enabled).apply()
        _compactMode.value = enabled
    }

    companion object {
        private const val KEY_COMPACT = "accounts_compact_mode"
    }
}
