package com.dashabi.control.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dashabi.control.AppContainer
import com.dashabi.control.data.CreditEntry
import com.dashabi.control.ui.theme.Amber
import com.dashabi.control.ui.theme.Ink
import com.dashabi.control.ui.theme.TextMuted
import com.dashabi.control.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONTokener

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun QqCreditDialog(
    container: AppContainer,
    accountCode: String,
    rescan: Boolean,
    onDismiss: () -> Unit,
    onSaved: (Int) -> Unit
) {
    var status by remember { mutableStateOf(if (rescan) "已清除登录，请重新扫码" else "正在打开信用分页面…") }
    var webRef by remember { mutableStateOf<WebView?>(null) }
    var lastScore by remember { mutableIntStateOf(0) }
    var stableTicks by remember { mutableIntStateOf(0) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(webRef) {
        val view = webRef ?: return@LaunchedEffect
        while (!saved) {
            delay(2000)
            view.evaluateJavascript("document.body ? document.body.innerText : ''") { raw ->
                if (saved) return@evaluateJavascript
                val text = runCatching {
                    val value = JSONTokener(raw).nextValue()
                    value as? String ?: ""
                }.getOrDefault("")
                val match = Regex("退出[\\s\\S]{0,80}?(\\d{1,4})").find(text)
                    ?: Regex("信用分[^0-9]{0,12}(\\d{2,4})").find(text)
                val score = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return@evaluateJavascript
                if (score !in 1..1000) return@evaluateJavascript
                if (score != lastScore) {
                    lastScore = score
                    stableTicks = 1
                    status = "读取中…当前 $score"
                    return@evaluateJavascript
                }
                stableTicks++
                if (stableTicks < 3) {
                    status = "分数定格中…$score"
                    return@evaluateJavascript
                }
                val cookies = CookieManager.getInstance().getCookie("https://gamecredit.qq.com")
                val nick = Regex("([^\\n]{2,20})\\n切换账号").find(text)?.groupValues?.getOrNull(1)?.trim()
                val oldEntry = container.credit.get(accountCode)
                val previous = when {
                    oldEntry == null -> null
                    oldEntry.score != score -> oldEntry.score
                    else -> oldEntry.previousScore
                }
                container.credit.put(
                    accountCode,
                    CreditEntry(score = score, nickname = nick, cookies = cookies, updatedAt = System.currentTimeMillis(), previousScore = previous)
                )
                // 登记后立即上传云端，电脑端同步时就能看到
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    runCatching { container.cloudSync.sync() }
                }
                saved = true
                status = "已登记：$score 分"
                onSaved(score)
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
            color = Ink
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("QQ 信用分", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text(status, color = if (saved) Amber else TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                Spacer(Modifier.height(6.dp))
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.userAgentString = settings.userAgentString
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            loadUrl("https://gamecredit.qq.com/static/web/index.html")
                            webRef = this
                        }
                    }
                )
            }
        }
    }
}
