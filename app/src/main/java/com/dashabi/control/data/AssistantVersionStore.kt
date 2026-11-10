package com.dashabi.control.data

import android.content.Context
import com.dashabi.control.remote.RebootBindStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 设备码 → 绑定的云控助手版本；另有云端最新助手版本，供设备总览/详情标红。 */
class AssistantVersionStore(
    private val context: Context,
    private val sync: CloudSync
) {
    private val _versions = MutableStateFlow<Map<String, String>>(emptyMap())
    val versions: StateFlow<Map<String, String>> = _versions.asStateFlow()

    private val _latest = MutableStateFlow<String?>(null)
    val latest: StateFlow<String?> = _latest.asStateFlow()

    private val _warnings = MutableStateFlow<Map<String, String>>(emptyMap())
    val warnings: StateFlow<Map<String, String>> = _warnings.asStateFlow()

    suspend fun refresh() {
        val latestVersion = runCatching { sync.assistantLatestVersion() }.getOrNull()
        val binds = RebootBindStore.toJson(context)
        val map = LinkedHashMap<String, String>()
        val warn = LinkedHashMap<String, String>()
        binds.keys().forEach { device ->
            val code = binds.optJSONObject(device)?.optString("code").orEmpty()
            if (code.isNotBlank()) {
                val status = runCatching { sync.assistantStatus(code) }.getOrNull()
                val version = status?.version?.takeIf { it.isNotBlank() }
                if (version != null) map[device] = version
                val limit = if (status?.accessibility == null) 8 * 60_000L else 2 * 60_000L
                val offline = status != null && (
                    status.accessibility == false ||
                        status.msg?.contains("无障碍") == true ||
                        (status.at > 0 && System.currentTimeMillis() - status.at > limit)
                    )
                if (offline) warn[device] = "助手无障碍已断开，扫码/远程操作不可用"
            }
        }
        _latest.value = latestVersion
        _versions.value = map
        _warnings.value = warn
    }
}
