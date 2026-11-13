package com.dashabi.control

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.dashabi.control.ui.nav.DashaRoot
import com.dashabi.control.ui.screens.AccountDetailScreen
import com.dashabi.control.ui.screens.DeviceDetailScreen
import com.dashabi.control.ui.theme.DashaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 真实组合（不依赖模拟器）的 UI 冒烟测试：
 * 校验两个主页面能完成 Compose 组合、底部导航可切换、详情页不会在组合阶段崩溃。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-xxhdpi")
class AppUiSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tabs_render_and_switch() {
        compose.setContent {
            DashaTheme {
                DashaRoot()
            }
        }

        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("设备总览").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("tab_accounts").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("账号总览").fetchSemanticsNodes().isNotEmpty()
        }
        // A clean installation has no service key; onboarding must remain reachable.
        compose.onNodeWithText("还没有配置 API Key").assertExists()
        compose.onNodeWithTag("tab_settings").performClick()
        compose.onNodeWithText("我的工作空间").assertExists()

        compose.onNodeWithTag("tab_devices").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("设备总览").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun chinese_strings_are_utf8() {
        compose.setContent {
            DashaTheme {
                DashaRoot()
            }
        }
        // 用 Unicode 转义而非字面量：若源码编码被误读，这个断言会失败
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("\u8bbe\u5907\u603b\u89c8").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tab_accounts").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("\u8d26\u53f7\u603b\u89c8").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun abnormal_tab_opens() {
        compose.setContent {
            DashaTheme {
                DashaRoot()
            }
        }
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("设备总览").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tab_abnormal").performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("异常中心").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("异常判定").assertExists()
    }

    @Test
    fun missing_key_opens_connection_settings() {
        compose.setContent { DashaTheme { DashaRoot() } }
        compose.onNodeWithText("去设置 Key").performClick()
        compose.onNodeWithText("我的工作空间").assertExists()
        compose.onNodeWithText("服务连接").performClick()
        compose.onNodeWithText("保存并测试连接").assertExists()
    }

    @Test
    fun light_theme_composes() {
        compose.setContent {
            DashaTheme(darkTheme = false) {
                DashaRoot()
            }
        }
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("设备总览").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tab_devices").assertExists()
    }

    @Test
    fun detail_screens_compose() {
        val container = ApplicationProvider.getApplicationContext<DashaApp>().container

        compose.setContent {
            DashaTheme {
                DeviceDetailScreen(
                    container = container,
                    deviceCode = "test-device-code",
                    onBack = {},
                    onOpenAccount = { _, _ -> }
                )
            }
        }
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("设备详情").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("控制").performClick()
        compose.onNodeWithText("设备控制").assertExists()
        compose.onNodeWithText("助手").performClick()
        // Assistant management must be available even when there is no sell task.
        compose.onNodeWithText("绑定助手设备").assertExists()
    }

    @Test
    fun account_detail_composes() {
        val container = ApplicationProvider.getApplicationContext<DashaApp>().container

        compose.setContent {
            DashaTheme {
                AccountDetailScreen(
                    container = container,
                    deviceCode = "test-device-code",
                    accountCode = "test-account-code",
                    onBack = {}
                )
            }
        }
        // 无论加载成功与否，页面标题或失败占位都应出现
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText("账号详情").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("读取失败").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
