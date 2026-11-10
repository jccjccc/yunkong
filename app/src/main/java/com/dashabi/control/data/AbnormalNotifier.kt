package com.dashabi.control.data

import android.Manifest
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dashabi.control.MainActivity
import com.dashabi.control.R
import com.dashabi.control.ui.vm.AbnormalAccount

/**
 * 异常账号相关通知（高重要性通道 → 手机顶部悬浮横幅）：
 *  - 新增异常账号
 *  - 异常解除
 * 点击通知直接打开 App 的「异常」页。
 */
class AbnormalNotifier(private val context: Context) {

    init {
        createChannel()
    }

    fun canNotify(): Boolean {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabled) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyNew(items: List<AbnormalAccount>) {
        if (items.isEmpty() || !canNotify()) return

        val shown = items.take(3)
        val title = if (items.size == 1) {
            "新增异常账号：${shown.first().displayName()}"
        } else {
            "新增 ${items.size} 个异常账号"
        }
        val summary = shown.joinToString("、") { it.displayName() }
        val bigText = buildString {
            shown.forEachIndexed { index, item ->
                if (index > 0) append('\n')
                append(item.displayName())
                append("：")
                append(item.reasons.joinToString("、"))
            }
            if (items.size > shown.size) {
                append("\n…共 ${items.size} 个")
            }
        }

        post(NEW_ID, title, if (items.size == 1) bigText else summary, bigText)
    }

    fun notifyAbnormalDevices(devices: List<Device>) {
        if (devices.isEmpty() || !canNotify()) return
        val names = devices.take(3).joinToString("、") { it.name ?: it.deviceCode.take(8) }
        val title = if (devices.size == 1) "设备异常：$names" else "${devices.size} 台设备异常"
        val body = devices.take(3).joinToString("\n") {
            val reasons = com.dashabi.control.ui.vm.deviceAbnormalReasons(it.status)
            "${it.name ?: it.deviceCode.take(8)}：${reasons.joinToString("、")}"
        }
        post(DEVICE_ID, title, body, body, tab = 0)
    }

    fun notifyRecovered(accounts: List<Account>) {
        if (accounts.isEmpty() || !canNotify()) return

        val names = accounts.take(3).joinToString("、") { it.displayName() }
        val title = if (accounts.size == 1) "异常已解除：$names" else "${accounts.size} 个账号已解除异常"
        val text = if (accounts.size == 1) {
            "已恢复正常，不再出现在异常页"
        } else {
            names + if (accounts.size > 3) " 等" else ""
        }

        post(RECOVERED_ID, title, text, text)
    }

    private fun post(id: Int, title: String, text: String, bigText: String, tab: Int = 1) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_TAB, tab)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pending)
            .addAction(0, "查看异常", pending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "异常账号提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "出现新增异常账号或异常解除时弹出悬浮提醒"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            enableLights(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
        }
        manager.createNotificationChannel(channel)
    }

    private fun AbnormalAccount.displayName(): String = account.displayName()

    private fun Account.displayName(): String =
        upstreamRemark?.trim()?.takeIf { it.isNotEmpty() }
            ?: gameNick?.trim()?.takeIf { it.isNotEmpty() }
            ?: accountCode

    companion object {
        private const val CHANNEL_ID = "abnormal_alerts_v2"
        private const val NEW_ID = 2001
        private const val RECOVERED_ID = 2002
        private const val DEVICE_ID = 2003
    }
}
