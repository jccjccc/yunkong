package com.dashabi.control.ui.stats

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * 内置「鼠鼠」战绩网页，注入脚本拦截页面接口返回，做到无感读取。
 * 现在带一个调试面板：显示最近一次接口返回的 JSON，用于确定字段名。
 */
class StatsWebActivity : ComponentActivity() {

    private var auto = false
    private var fallbackShown = false
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gameNick = intent.getStringExtra("game_nick").orEmpty()
        auto = gameNick.isNotBlank() && intent.getBooleanExtra("auto", false)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0E0E0E.toInt())
        }

        fun roundBtn(label: String, primary: Boolean, onClick: android.view.View.OnClickListener): android.widget.Button {
            return android.widget.Button(this).apply {
                text = label
                textSize = 16f
                setTextColor(if (primary) 0xFFFFFFFF.toInt() else 0xFF1F1F1F.toInt())
                stateListAnimator = null
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 48f
                    setColor(if (primary) 0xFFC97D1B.toInt() else 0xF7FFFFFF.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(0, 118, 1f).apply {
                    marginStart = 10
                    marginEnd = 10
                }
                setOnClickListener(onClick)
            }
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(28, 14, 28, 28)
            setBackgroundColor(0x00000000)
        }
        bar.addView(roundBtn("返回", primary = false) {
            val data = android.content.Intent().putExtra("stats", "")
            setResult(RESULT_OK, data)
            finish()
        })
        bar.addView(roundBtn("填入游戏名", primary = true) { fillAndQuery(gameNick) })

        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        if (auto) {
            // 真正静默：窗口缩到 1px 并移到屏幕外，用户完全看不到
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0x00000000))
            window.setDimAmount(0f)
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            val lp = window.attributes
            lp.width = 1
            lp.height = 1
            lp.x = 0
            lp.y = 0
            window.attributes = lp
            root.setBackgroundColor(0x00000000)
            root.alpha = 0f
        }
        setContentView(root)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        // 用桌面版 UA，页面按 PC 布局渲染
        web.settings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = true
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        web.settings.setSupportZoom(true)
        web.addJavascriptInterface(Bridge(), "DashaBridge")
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                web.evaluateJavascript(CAPTURE_JS, null)
                if (auto && gameNick.isNotBlank()) {
                    // 无感模式：等页面渲染完自动填入并查询
                    web.postDelayed({ fillAndQuery(gameNick) }, 2500)
                }
            }
        }
        web.loadUrl("https://shushu.fan/achievement")

        if (auto) {
            web.postDelayed({
                if (!fallbackShown && !isFinishing) {
                    fallbackShown = true
                    window.setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val lp2 = window.attributes
                    lp2.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    lp2.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    window.attributes = lp2
                    root.setBackgroundColor(0xFF111111.toInt())
                    root.alpha = 1f
                                                        }
            }, 25_000)
        }
    }

    /** 自动填入游戏名并点查询；返回诊断信息方便排查选择器。 */
    private fun fillAndQuery(gameNick: String) {
        if (gameNick.isBlank()) return
        val js = """
(function(){
 try {
  var out = [];
  var inputs = Array.from(document.querySelectorAll('input, textarea'));
  out.push('inputs=' + inputs.length);
  inputs.slice(0,6).forEach(function(i){ out.push('PH[' + (i.placeholder||'') + ']TY[' + (i.type||'') + ']'); });
  var t = inputs.find(function(i){ return /玩家名|输入/.test(i.placeholder||'') }) || inputs[0];
  if (!t) { out.push('NO_INPUT'); window.DashaBridge.onDiag(out.join(' | ')); return; }
  try {
    t.focus(); t.click();
    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;
    setter.call(t, __NAME__);
    ['input','change','keyup','blur'].forEach(function(e){ t.dispatchEvent(new Event(e,{bubbles:true})); });
    out.push('set=' + t.value);
  } catch(e) { out.push('setErr=' + e); }
  var els = Array.from(document.querySelectorAll('button,div,span,a'));
  var q = els.find(function(b){ return (b.innerText||'').trim() === '查询战绩' })
       || els.find(function(b){ return /查询战绩/.test(b.innerText||'') });
  out.push('btn=' + (q ? 'yes' : 'no'));
  if (q) { try { q.click(); } catch(e) { out.push('clickErr=' + e); } }
  window.DashaBridge.onDiag(out.join(' | '));
 } catch (e) { try { window.DashaBridge.onDiag('JS_ERROR: ' + e); } catch(e2){} }
})()
""".trimIndent()
        web.evaluateJavascript(js.replace("__NAME__", org.json.JSONObject.quote(gameNick)), null)
        // 页面查完后，从 performance 里找出战绩请求并直接取数据
        listOf(6000L, 12000L, 20000L).forEach { delay ->
            web.postDelayed({
                web.evaluateJavascript(PERF_POLL) { r ->
                    android.util.Log.i("DashaStats", "poll=" + r)
                }
            }, delay)
        }
    }

    private var parsed: String? = null

    /** 查询后从页面 performance 里找出战绩请求，再自己取一次数据（最稳）。 */
    private val PERF_POLL = """
(function(){
  try {
    var urls = performance.getEntriesByType('resource').map(function(e){ return e.name; });
    var dataUrl = urls.filter(function(u){ return u.indexOf('/achievement/data/') >= 0; }).pop();
    var statusUrl = urls.filter(function(u){ return u.indexOf('/achievement/status/') >= 0; }).pop();
    var target = dataUrl;
    if (!target && statusUrl) {
      var bid = statusUrl.split('/').pop();
      target = location.origin + '/api/achievement/data/' + bid;
    }
    if (!target) { return 'no-url'; }
    fetch(target, {credentials:'include'}).then(function(r){ return r.text(); })
      .then(function(t){ window.DashaBridge.onData(t); });
    return 'polling';
  } catch(e) { return 'err:' + e; }
})()
""".trimIndent()

    /** 从接口 JSON 里抽出要的 5 个字段。 */
    private fun parseAchievement(json: String): String? {
        if (!json.contains("game_data")) return null
        return runCatching {
            val o = org.json.JSONObject(json).optJSONObject("data") ?: return null
            val g = o.optJSONObject("game_data") ?: return null
            val kd = g.optJSONObject("combat")?.optJSONObject("kd_ratio")
            val eco = g.optJSONObject("economy")
            val meta = g.optJSONObject("metadata")
            val season = g.optJSONObject("season_data")
            val recent = g.optJSONArray("recent_battles")
            val lastGame = if (recent != null && recent.length() > 0) {
                val r = recent.getJSONObject(0)
                (r.optString("date") + " " + r.optString("time")).trim()
            } else null
            val rankText = when {
                (season?.optString("rank_score") ?: "0") == "0" -> "未上榜"
                else -> "分数 " + season?.optString("rank_score")
            }
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

    inner class Bridge {
        @JavascriptInterface
        fun onDiag(text: String) {
            runOnUiThread {
                                            }
        }

        @JavascriptInterface
        fun onData(json: String) {
            val text = parseAchievement(json)
            if (text != null) {
                StatsResultBus.latest = text
                return
            }
            runOnUiThread {
                if (text != null) {
                    parsed = text
                                                        } else {
                                    }
            }
        }
    }

    companion object {
        fun intent(context: Context, gameNick: String?, auto: Boolean = false): Intent =
            Intent(context, StatsWebActivity::class.java)
                .putExtra("game_nick", gameNick.orEmpty())
                .putExtra("auto", auto)

        /** 在页面最早时机包住 fetch / XHR，把接口返回交给原生侧。 */
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
  var oo = XMLHttpRequest.prototype.open;
  var os = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function(){ this.__dashaUrl = arguments[1]; return oo.apply(this, arguments); };
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
}
