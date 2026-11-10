package com.dashabi.control

import android.app.Application
import android.content.Context
import kotlinx.coroutines.launch
import coil.ImageLoader
import kotlinx.coroutines.launch
import coil.ImageLoaderFactory
import com.dashabi.control.data.AbnormalNotifier
import com.dashabi.control.data.AiSettingsStore
import com.dashabi.control.data.AssistantVersionStore
import com.dashabi.control.data.AutoSellStore
import com.dashabi.control.data.CloudSync
import com.dashabi.control.data.CreditStore
import com.dashabi.control.data.GameNameStore
import com.dashabi.control.data.Repository
import com.dashabi.control.data.SettingsStore
import com.dashabi.control.data.StaminaStore
import com.dashabi.control.data.ThemeStore
import com.dashabi.control.data.UiPrefsStore
import com.dashabi.control.data.remote.DashaApi
import com.dashabi.control.data.remote.DeepSeekVision

class AppContainer(context: Context) {
    val settings = SettingsStore(context)
    val api = DashaApi(settings)
    val repo = Repository(api)
    val aiSettings = AiSettingsStore(context)
    val stamina = StaminaStore(context)
    val theme = ThemeStore(context)
    val uiPrefs = UiPrefsStore(context)
    val vision = DeepSeekVision()
    val notifier = AbnormalNotifier(context)
    val autoSell = AutoSellStore(context)
    val credit = CreditStore(context)
    val hafHistory = com.dashabi.control.data.HafHistoryStore(context)
    val deviceOrder = com.dashabi.control.data.DeviceOrderStore(context)
    val gameNames = GameNameStore(context)
    val cloudSync = CloudSync(context, stamina, credit, gameNames)
    val assistantVersions = AssistantVersionStore(context, cloudSync)
}

class DashaApp : Application(), ImageLoaderFactory {

    companion object {
        /** 与界面无关的后台任务，扫码结束后兜底恢复定时用。 */
        val timerRestoreScope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
        )
    }


    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            kotlinx.coroutines.delay(2000)
            runCatching { CloudSync(this@DashaApp, container.stamina, container.credit, container.gameNames).sync() }
        }
    }

    /** 图片加载复用带鉴权头的 OkHttpClient。 */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { container.api.httpClient }
        .crossfade(160)
        .build()
}
