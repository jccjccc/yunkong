package com.dashabi.control.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dashabi.control.data.Account
import com.dashabi.control.data.ApiResult
import com.dashabi.control.data.CreditEntry
import com.dashabi.control.data.CreditStore
import com.dashabi.control.data.Repository
import com.dashabi.control.data.StaminaStore
import com.dashabi.control.data.SyncPayload
import com.dashabi.control.data.encodeSyncMarker
import com.dashabi.control.data.parseSyncMarker
import com.dashabi.control.data.stripSyncMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncViewModel(
    private val repo: Repository,
    private val stamina: StaminaStore,
    private val credit: CreditStore
) : ViewModel() {

    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set

    fun sync() {
        if (busy) return
        busy = true
        message = "正在同步…"
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runSync() }
            busy = false
            message = result
            SyncStatus.last = result
        }
    }

    private suspend fun runSync(): String {
        val accounts = when (val r = repo.accounts()) {
            is ApiResult.Ok -> r.value
            is ApiResult.Err -> return "读取账号失败：${r.message}"
        }
        var uploaded = 0
        var downloaded = 0
        var failed = 0
        for (account in accounts) {
            val device = account.deviceCode ?: continue
            val code = account.accountCode
            val remote = parseSyncMarker(account.websiteNote)
            if (remote != null && !remote.isEmpty) {
                stamina.put(code, remote.training, remote.range, remote.aw)
                remote.credit?.let {
                    credit.put(code, CreditEntry(it, null, null, System.currentTimeMillis()))
                }
                downloaded++
                continue
            }
            val levels = stamina.get(code)
            val creditEntry = credit.get(code)
            val payload = SyncPayload(
                training = levels?.training,
                range = levels?.range,
                aw = levels?.awBullets,
                credit = creditEntry?.score
            )
            if (payload.isEmpty) continue
            val base = stripSyncMarker(account.websiteNote)
            val marker = encodeSyncMarker(payload)
            val note = if (base.isBlank()) marker else "$base\n$marker"
            when (val r = repo.setNote(device, code, note)) {
                is ApiResult.Ok -> uploaded++
                is ApiResult.Err -> failed++
            }
        }
        return buildString {
            append("同步完成：上传 $uploaded 个账号，读取 $downloaded 个账号")
            if (failed > 0) append("，失败 $failed 个")
        }
    }
}


/** 最近一次同步的结果，供设置页显示。 */
object SyncStatus {
    @Volatile
    var last: String? = null
}
