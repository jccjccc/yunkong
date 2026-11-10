package com.dashabi.control.ui.stats

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

/**
 * 完全静默的战绩抓取：在后台用不可见的 WebView 打开战绩页，
 * 自动填游戏名、点查询，拦截页面接口返回并解析出 5 个字段。不显示任何界面。
 */
object StatsFetcher {

    private const val URL = "https://shushu.fan/achievement"
    private const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    fun fetch(context: Context, gameNick: String, callback: (String?) -> Unit) {
        if (gameNick.isBlank()) {
            callback(null)
            return
        }
        var done = false
        var web: WebView? = null

        fun finish(result: String?) {
            if (done) return
            done = true
            handler.post {
                runCatching { web?.destroy() }
            }
            callback(result)
        }

        handler.post {
            val ctx = ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault)
            val w = WebView(ctx)
            web = w
            w.settings.javaScriptEnabled = true
            w.settings.domStorageEnabled = true
            w.settings.userAgentString = DESKTOP_UA
            w.settings.loadWithOverviewMode = true
            w.settings.useWideViewPort = true
            w.webChromeClient = WebChromeClient()
            w.addJavascriptInterface(object {
                @JavascriptInterface
                fun onData(json: String) {
                    val parsed = parse(json) ?: return
                    finish(parsed)
                }
            }, "DashaBridge")
            w.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    w.evaluateJavascript(CAPTURE_JS, null)
                    handler.postDelayed({
                        if (!done) w.evaluateJavascript(fillJs(gameNick), null)
                    }, 2500)
                }
            }
            w.loadUrl(URL)

            // 超时
            handler.postDelayed({ finish(null) }, 45_000)
        }
    }

    private fun fillJs(name: String): String = """
(function(){
  var name = ${'"'}${name.replace("\\", "\\\\").replace("\"", "\\\"")}${'"'};
  var inputs = Array.from(document.querySelectorAll('input'));
  var t = inputs.find(function(i){ return /玩家名|输入/.test(i.placeholder||'') }) || inputs[0];
  if (!t) return 'no-input';
  var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;
  setter.call(t, name);
  t.dispatchEvent(new Event('input',{bubbles:true}));
  t.dispatchEvent(new Event('change',{bubbles:true}));
  var btns = Array.from(document.querySelectorAll('button,div,span'));
  var q = btns.find(function(b){ return (b.innerText||'').trim() === '查询战绩' });
  if (q) q.click();
  return q ? 'clicked' : 'no-button';
})()
""".trimIndent()

    /** 解析接口 JSON → 5 个字段文本。 */
    fun parse(json: String): String? {
        if (!json.contains("game_data")) return null
        return runCatching {
            val data = JSONObject(json).optJSONObject("data") ?: return null
            val g = data.optJSONObject("game_data") ?: return null
            val kd = g.optJSONObject("combat")?.optJSONObject("kd_ratio")
            val eco = g.optJSONObject("economy")
            val meta = g.optJSONObject("metadata")
            val season = g.optJSONObject("season_data")
            val recent = g.optJSONArray("recent_battles")
            val lastGame = if (recent != null && recent.length() > 0) {
                val r = recent.getJSONObject(0)
                (r.optString("date") + " " + r.optString("time")).trim()
            } else null
            val rankText = if ((season?.optString("rank_score") ?: "0") == "0") "未上榜"
            else "分数 " + season?.optString("rank_score")
            listOf(
                "段位：" + rankText,
                "战损比：" + listOf(
                    kd?.optString("normal") ?: "—",
                    kd?.optString("classified") ?: "—",
                    kd?.optString("top_secret") ?: "—"
                ).joinToString(" / "),
                "流动资产：" + (eco?.optString("liquid_assets") ?: "—"),
                "最后一场：" + (lastGame ?: "—"),
                "更新时间：" + (meta?.optString("updated_at") ?: "—")
            ).joinToString(System.lineSeparator())
        }.getOrNull()
    }

    private val CAPTURE_JS = """
(function(){
  if (window.__dashaHooked) return; window.__dashaHooked = true;
  function report(s){
    try { if (s && s.length > 40 && (s[0]=='{' || s[0]=='[')) window.DashaBridge.onData(s); } catch(e){}
  }
  var of = window.fetch;
  window.fetch = function(){
    return of.apply(this, arguments).then(function(r){
      try { r.clone().text().then(report); } catch(e){}
      return r;
    });
  };
  var oo = XMLHttpRequest.prototype.open, os = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function(){ return oo.apply(this, arguments); };
  XMLHttpRequest.prototype.send = function(){
    var self = this;
    this.addEventListener('load', function(){
      try { report(String(self.responseText||'')); } catch(e){}
    });
    return os.apply(this, arguments);
  };
})()
""".trimIndent()
}
