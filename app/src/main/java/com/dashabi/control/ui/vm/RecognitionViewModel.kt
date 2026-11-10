package com.dashabi.control.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dashabi.control.data.AiSettings
import com.dashabi.control.data.AiSettingsStore
import com.dashabi.control.data.Account
import com.dashabi.control.data.ApiResult
import com.dashabi.control.data.Repository
import com.dashabi.control.data.remote.ImageFetchException
import com.dashabi.control.data.StaminaStore
import com.dashabi.control.data.remote.DeepSeekVision
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** 识别范围：体力负重、AW 子弹，或两者一起。 */
enum class RecognizeMode {
    LEVELS,
    AW,
    BOTH
}

/**
 * 批量识别：
 *  - 特勤处截图 → 训练中心 / 靶场等级（体力 / 负重）
 *  - 仓库截图 → .338AP（AW）子弹数量
 * 结果写入 [StaminaStore]，供列表、缩略模式与详情展示。
 */
class RecognitionViewModel(
    private val repo: Repository,
    private val aiSettings: AiSettingsStore,
    private val staminaStore: StaminaStore,
    private val vision: DeepSeekVision
) : ViewModel() {

    var running by mutableStateOf(false); private set
    var total by mutableStateOf(0); private set
    var done by mutableStateOf(0); private set
    var succeeded by mutableStateOf(0); private set
    var failed by mutableStateOf(0); private set
    var currentName by mutableStateOf(""); private set
    var error by mutableStateOf<String?>(null); private set
    var finished by mutableStateOf(false); private set
    var mode by mutableStateOf(RecognizeMode.BOTH); private set

    private var job: Job? = null
    private val imageGate = Mutex()
    private var lastImageAt = 0L
    private val IMAGE_INTERVAL_MS = 700L

    fun start(onlyMissing: Boolean, mode: RecognizeMode = RecognizeMode.BOTH) {
        if (running) return
        this.mode = mode
        val settings = aiSettings.current()
        if (settings.apiKey.isBlank()) {
            error = "请先在设置里填写 DeepSeek API Key"
            finished = true
            return
        }

        job = viewModelScope.launch {
            running = true
            finished = false
            error = null
            total = 0
            done = 0
            succeeded = 0
            failed = 0
            try {
                when (val result = repo.accounts()) {
                    is ApiResult.Err -> error = result.message
                    is ApiResult.Ok -> {
                        val targets = result.value
                            .filter { account ->
                                account.deviceCode != null && when (mode) {
                                    RecognizeMode.LEVELS -> account.images?.tqc?.available == true
                                    RecognizeMode.AW -> account.images?.store?.available == true
                                    RecognizeMode.BOTH ->
                                        account.images?.tqc?.available == true ||
                                            account.images?.store?.available == true
                                }
                            }
                            .filter { !onlyMissing || needsRecognition(it, mode) }
                        total = targets.size
                        if (targets.isNotEmpty()) {
                            val semaphore = Semaphore(3)
                            coroutineScope {
                                targets.forEach { account ->
                                    launch {
                                        semaphore.withPermit {
                                            currentName = account.gameNick ?: account.accountCode
                                            val ok = process(account, settings, onlyMissing, mode)
                                            done += 1
                                            if (ok) succeeded += 1 else failed += 1
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                error = "已取消（已完成 $done/$total）"
            } catch (e: Exception) {
                error = e.message ?: "识别失败"
            } finally {
                running = false
                finished = true
                currentName = ""
            }
        }
    }

    /** 只识别未登记：按模式只检查对应的字段，已有数据不会被清空。 */
    private fun needsRecognition(account: Account, mode: RecognizeMode): Boolean {
        val existing = staminaStore.get(account.accountCode)
        val levelsMissing = account.images?.tqc?.available == true &&
            (existing == null || existing.training == null || existing.range == null)
        val awMissing = account.images?.store?.available == true &&
            (existing == null || existing.awBullets == null ||
                existing.awVersion < StaminaStore.AW_PROMPT_VERSION)
        return when (mode) {
            RecognizeMode.LEVELS -> levelsMissing
            RecognizeMode.AW -> awMissing
            RecognizeMode.BOTH -> levelsMissing || awMissing
        }
    }

    private suspend fun process(
        account: Account,
        settings: AiSettings,
        onlyMissing: Boolean,
        mode: RecognizeMode
    ): Boolean {
        val device = account.deviceCode ?: return false
        var saved = false
        val existing = staminaStore.get(account.accountCode)

        // 「只识别未登记」时跳过已经完整的部分，避免为补 AW 重新识别一遍特勤处
        val doTqc = mode != RecognizeMode.AW &&
            account.images?.tqc?.available == true &&
            (!onlyMissing || existing == null || existing.training == null || existing.range == null)
        val doStore = mode != RecognizeMode.LEVELS &&
            account.images?.store?.available == true &&
            (!onlyMissing ||
                existing == null ||
                existing.awBullets == null ||
                existing.awVersion < StaminaStore.AW_PROMPT_VERSION)

        if (doTqc) {
            try {
                val bytes = downloadImage(device, account.accountCode, "tqc")
                val levels = vision.recognize(bytes, settings)
                    ?: throw IllegalStateException("未识别到训练中心 / 靶场")
                if (levels.training == null && levels.range == null) {
                    throw IllegalStateException("未识别到训练中心 / 靶场")
                }
                staminaStore.put(account.accountCode, levels.training, levels.range, null)
                saved = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                recordError(account, "特勤处", e)
            }
        }

        if (doStore) {
            try {
                val bytes = downloadImage(device, account.accountCode, "store")
                val aw = vision.recognizeBullets(bytes, settings)
                staminaStore.put(account.accountCode, null, null, aw)
                saved = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                recordError(account, "仓库", e)
            }
        }

        return saved
    }

    private fun recordError(account: Account, label: String, e: Exception) {
        if (error != null) return
        val name = account.gameNick ?: account.accountCode
        error = if (e is ImageFetchException) {
            "$name：${label}图片下载失败（${e.message}）"
        } else {
            "$name：${label}识别失败（${e.message ?: "未知错误"}）"
        }
    }

    /**
     * 图片接口限流 120 次/分钟：每个账号要下载特勤处 + 仓库两张图，
     * 这里把下载开始时间间隔控制在 700ms 左右（约 85 张/分钟），
     * 遇到 429 时 Repository 还会按 Retry-After 重试。
     */
    private suspend fun downloadImage(
        deviceCode: String,
        accountCode: String,
        kind: String
    ): ByteArray {
        imageGate.withLock {
            val wait = IMAGE_INTERVAL_MS - (System.currentTimeMillis() - lastImageAt)
            if (wait > 0) delay(wait)
            lastImageAt = System.currentTimeMillis()
        }
        return if (kind == "store") {
            repo.storeImageBytes(deviceCode, accountCode)
        } else {
            repo.tqcImageBytes(deviceCode, accountCode)
        }
    }

    fun cancel() {
        job?.cancel()
    }

    fun dismiss() {
        finished = false
        error = null
        total = 0
        done = 0
        succeeded = 0
        failed = 0
    }
}
