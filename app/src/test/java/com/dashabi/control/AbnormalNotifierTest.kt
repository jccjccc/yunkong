package com.dashabi.control

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dashabi.control.data.AbnormalNotifier
import com.dashabi.control.data.Account
import com.dashabi.control.ui.vm.AbnormalAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AbnormalNotifierTest {

    private fun item(code: String = "10001") = AbnormalAccount(
        account = Account(accountCode = code, gameNick = "测试账号"),
        reasons = listOf("哈夫币不足"),
        hafCoin = 1.0
    )

    @Test
    fun channel_uses_high_importance_for_heads_up() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AbnormalNotifier(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel("abnormal_alerts_v2")
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    }

    @Test
    fun posts_notification_when_permission_granted() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val notifier = AbnormalNotifier(app)
        assertTrue(notifier.canNotify())

        notifier.notifyNew(listOf(item()))
        val manager = app.getSystemService(NotificationManager::class.java)
        assertEquals(1, shadowOf(manager).size())
    }

    @Test
    fun posts_recovery_notification() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val notifier = AbnormalNotifier(app)
        notifier.notifyRecovered(
            listOf(Account(accountCode = "10001", gameNick = "测试账号"))
        )

        val manager = app.getSystemService(NotificationManager::class.java)
        assertEquals(1, shadowOf(manager).size())
    }
}