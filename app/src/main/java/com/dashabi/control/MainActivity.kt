package com.dashabi.control

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.dashabi.control.data.License
import com.dashabi.control.data.ThemeMode
import com.dashabi.control.ui.nav.DashaRoot
import com.dashabi.control.ui.theme.DashaTheme
import kotlinx.serialization.json.jsonObject

class MainActivity : ComponentActivity() {

    private val pendingTab = mutableIntStateOf(0)
    private val tabRequest = mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialTab = intent?.getIntExtra(EXTRA_TAB, 0) ?: 0
        pendingTab.intValue = initialTab
        if (initialTab != 0) tabRequest.intValue = 1
        requestNotificationPermission()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val container = (application as DashaApp).container
            val mode by container.theme.mode.collectAsState()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            val license = remember { License(applicationContext) }
            var locked by remember { mutableStateOf(false) }
            var checkTick by remember { mutableStateOf(0) }
            var pendingUpdate by remember { mutableStateOf<UpdateNotice?>(null) }

            LaunchedEffect(Unit) {
                val since = mutableMapOf<String, Long>()
                val lastReboot = mutableMapOf<String, Long>()
                while (true) {
                    runCatching {
                        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            container.repo.devices()
                        }
                        if (result is com.dashabi.control.data.ApiResult.Ok) {
                            val now = System.currentTimeMillis()
                            val current = result.value.filter { it.status?.code == "accessibility_offline" }
                            since.keys.retainAll(current.mapNotNull { it.deviceCode }.toSet())
                            for (device in current) {
                                val code = device.deviceCode ?: continue
                                val start = since.getOrPut(code) { now }
                                if (now - start < 180_000L) continue
                                if (now - (lastReboot[code] ?: 0L) < 600_000L) continue
                                val assistant = com.dashabi.control.remote.RebootBindStore.get(applicationContext, code).orEmpty()
                                if (assistant.isBlank()) continue
                                lastReboot[code] = now
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    container.cloudSync.sendReboot(assistant)
                                }
                            }
                        }
                    }
                    kotlinx.coroutines.delay(30_000L)
                }
            }

            LaunchedEffect(checkTick) {
                while (true) {
                    val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { license.whitelisted() }.getOrDefault(false)
                    }
                    locked = !ok && license.expired()
                    kotlinx.coroutines.delay(if (locked) 15_000L else 7_200_000L)
                }
            }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(4000)
                runCatching {
                    val text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        java.net.URL("https://dasha-update-1313972671.cos.ap-shanghai.myqcloud.com/latest-android.json")
                            .openStream().bufferedReader().readText()
                    }
                    val o = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject
                    UpdateNotice(
                        version = (o["version"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                        url = (o["url"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                        sha256 = (o["sha256"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(),
                        notes = (o["notes"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                    )
                }.onSuccess { info ->
                    if (versionGreater(info.version, BuildConfig.VERSION_NAME.substringBefore("-"))) {
                        // 打开 App 发现有更新：直接后台下载并拉起安装器
                        android.widget.Toast.makeText(
                            applicationContext,
                            "发现新版本 ${info.version}，正在自动下载…",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        downloadAndInstall(applicationContext, info.url, info.sha256)
                    }
                }
            }

            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxSize().then(if (locked) Modifier.blur(14.dp) else Modifier)
                ) {
                    DashaTheme(darkTheme = dark) {
                        DashaRoot(initialTab = pendingTab.intValue, tabRequest = tabRequest.intValue)
                    }
                }
                if (locked) {
                    val clipboard = LocalClipboardManager.current
                    Box(
                        Modifier.fillMaxSize()
                            .background(UiColor.Black.copy(alpha = 0.92f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("试用已结束，请联系管理员", color = UiColor.White)
                            Text("设备码：${license.deviceCode}", color = UiColor.White)
                            androidx.compose.material3.Button(onClick = {
                                clipboard.setText(AnnotatedString(license.deviceCode))
                            }) { Text("复制设备码") }
                            Spacer(Modifier.height(10.dp))
                            androidx.compose.material3.OutlinedButton(onClick = { checkTick++ }) {
                                Text("重新检查授权")
                            }
                        }
                    }
                }
            }

            pendingUpdate?.let { info ->
                AlertDialog(
                    onDismissRequest = { pendingUpdate = null },
                    title = { Text("发现新版本 ${info.version}") },
                    text = { Text(info.notes.ifBlank { "有新版本可用，建议更新。" }) },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingUpdate = null
                            downloadAndInstall(applicationContext, info.url, info.sha256)
                        }) { Text("立即更新") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingUpdate = null }) { Text("稍后") }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTab.intValue = intent.getIntExtra(EXTRA_TAB, 0)
        tabRequest.intValue = tabRequest.intValue + 1
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_TAB = "extra_tab"
    }
}

private data class UpdateNotice(
    val version: String,
    val url: String,
    val sha256: String,
    val notes: String
)

private fun versionGreater(a: String, b: String): Boolean {
    val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
    val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val x = pa.getOrElse(i) { 0 }
        val y = pb.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

internal fun downloadAndInstall(
    context: android.content.Context,
    url: String,
    sha: String,
    onStatus: (String) -> Unit = {},
    onFinished: () -> Unit = {}
) {
    fun toast(msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onStatus(msg)
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    Thread {
        try {
            val bytes = java.net.URL(url).openStream().use { it.readBytes() }
            if (sha.isNotBlank()) {
                val actual = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(bytes).joinToString("") { "%02x".format(it) }
                if (!actual.equals(sha, ignoreCase = true)) {
                    toast("更新包校验失败，请重试")
                    return@Thread
                }
            }
            // 更新包可能是「包了一层 zip 的 APK」，也可能就是 APK 本身（APK 也是 zip 结构，
            // 所以必须先看里面有没有 .apk 条目，再决定用哪个）。
            val looksZip = bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
            val innerApk = if (looksZip) {
                try {
                    java.util.zip.ZipInputStream(bytes.inputStream()).use { zin ->
                        var entry = zin.nextEntry
                        var out: ByteArray? = null
                        while (entry != null) {
                            if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                                out = zin.readBytes(); break
                            }
                            entry = zin.nextEntry
                        }
                        out
                    }
                } catch (e: Exception) {
                    null
                }
            } else null
            val apkBytes = innerApk ?: bytes
            if (apkBytes.size < 4096) {
                toast("更新包内容异常，请到官网重新下载")
                return@Thread
            }
            val file = java.io.File(context.cacheDir, "update.apk")
            file.writeBytes(apkBytes)

            // 交给安装器之前先自己解析一遍，避免把坏包丢给系统导致「解析软件包时出现问题」
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getPackageArchiveInfo(file.absolutePath, 0)
            if (info == null) {
                toast("安装包解析失败，请用原来的安装包手动更新")
                return@Thread
            }
            if (info.packageName != context.packageName) {
                toast("安装包包名不匹配")
                return@Thread
            }
            @Suppress("DEPRECATION")
            val self = pm.getPackageInfo(context.packageName, 0)
            val newCode = if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            val curCode = if (android.os.Build.VERSION.SDK_INT >= 28) self.longVersionCode else self.versionCode.toLong()
            if (newCode <= curCode) {
                toast("安装包版本不高于当前版本")
                return@Thread
            }

            if (android.os.Build.VERSION.SDK_INT >= 26 &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                toast("请先允许「安装未知应用」，再点一次更新")
                try {
                    context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            android.net.Uri.parse("package:" + context.packageName)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                }
                return@Thread
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, context.packageName + ".fileprovider", file
            )
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            toast("安装包已校验，已打开系统安装器")
        } catch (e: Exception) {
            toast("更新失败：" + (e.message ?: e.javaClass.simpleName))
        } finally {
            android.os.Handler(android.os.Looper.getMainLooper()).post { onFinished() }
        }
    }.start()
}
