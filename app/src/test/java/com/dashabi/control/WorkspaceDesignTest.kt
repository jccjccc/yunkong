package com.dashabi.control

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dashabi.control.ui.components.*
import com.dashabi.control.ui.screens.SettingsScreen
import com.dashabi.control.ui.screens.RecognitionStatusDialog
import com.dashabi.control.ui.screens.RecognitionProgressContent
import com.dashabi.control.ui.screens.ScheduleDialog
import com.dashabi.control.ui.screens.NoteDialog
import com.dashabi.control.ui.vm.RecognizeMode
import com.dashabi.control.ui.theme.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Offline rendering and interactions: never invoke installation or remote commands. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WorkspaceDesignTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun settings(dark: Boolean = true, scale: Float = 1f): AppContainer {
        val container = AppContainer(compose.activity)
        container.settings.save("", "https://example.invalid")
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                DashaTheme(dark) { SettingsScreen(container, {}, {}, { _, _ -> }, onSelectTab = {}) }
            }
        }
        return container
    }

    @Test fun settings_home_dark() {
        settings()
        compose.onNodeWithText("我的工作空间").assertIsDisplayed()
        screenshot("settings-dark")
    }
    @Test fun settings_home_light() { settings(false); screenshot("settings-light") }
    @Test @Config(qualifiers = "w320dp-h740dp-mdpi")
    fun connection_narrow_large_font() {
        settings(scale = 1.3f)
        compose.onNodeWithText("服务连接").performClick()
        compose.onNodeWithText("保存并测试连接").assertIsDisplayed().assertIsNotEnabled()
        screenshot("settings-connection-320")
    }
    @Test fun appearance_page() {
        settings()
        compose.onNodeWithText("外观设置").performScrollTo().performClick()
        screenshot("settings-appearance")
    }
    @Test fun client_update_page() {
        settings()
        compose.onNodeWithText("版本更新").performScrollTo().performClick()
        compose.onNodeWithText("检查更新").assertIsDisplayed()
        compose.onNodeWithText("下载并安装更新").assertDoesNotExist()
        screenshot("settings-update")
    }
    @Test fun settings_back_returns_to_hub() {
        settings()
        compose.onNodeWithText("使用帮助").performScrollTo().performClick()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("我的工作空间").performScrollTo().assertIsDisplayed()
    }
    @Test fun saving_connection_persists_entered_values() {
        val container = settings()
        compose.onNodeWithText("服务连接").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("offline-design-key")
        compose.onNodeWithText("保存设置").performClick()
        compose.runOnIdle { assertEquals("offline-design-key", container.settings.current().apiKey) }
        compose.onNodeWithText("我的工作空间").assertIsDisplayed()
    }
    @Test fun update_install_and_recheck_have_distinct_callbacks() {
        var installs = 0
        var checks = 0
        compose.setContent { DashaTheme { UpdatePanel("1.0", "1.1", "修复连接与显示问题", false, null, { checks++ }, { installs++ }) } }
        compose.onNodeWithText("下载并安装更新").performClick()
        compose.runOnIdle { assertEquals(1, installs); assertEquals(0, checks) }
        compose.onNodeWithText("重新检查版本").performClick()
        compose.runOnIdle { assertEquals(1, checks); assertEquals(1, installs) }
    }
    @Test fun busy_update_disables_both_actions() {
        compose.setContent { DashaTheme { UpdatePanel("1.0", "1.1", "", true, "正在下载并校验安装包…", {}, {}) } }
        compose.onNodeWithText("处理中…").assertIsNotEnabled()
        compose.onNodeWithText("重新检查版本").assertIsNotEnabled()
        screenshot("update-busy")
    }
    @Test fun detail_sections_switch_selection() {
        compose.setContent {
            var selected by remember { mutableIntStateOf(0) }
            DashaTheme { DetailSectionTabs(listOf("概览", "控制", "助手", "账号"), selected, { selected = it }) }
        }
        compose.onNodeWithText("助手").performClick().assertIsSelected()
        compose.onNodeWithText("概览").assertIsNotSelected()
    }
    @Test fun primary_navigation_keeps_four_destinations_and_correct_callbacks() {
        val selections = mutableListOf<Int>()
        compose.setContent { DashaTheme { DashaBottomBar(3, Icons.Outlined.Devices, Icons.Outlined.Person, Icons.Outlined.WarningAmber, 3, 2, { selections.add(it) }) } }
        val tags = listOf("tab_devices", "tab_accounts", "tab_abnormal", "tab_settings")
        val bounds = tags.map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.left }
        assertTrue(bounds.zipWithNext().all { (left, right) -> left < right })
        tags.forEach { compose.onNodeWithTag(it).performClick() }
        compose.runOnIdle { assertEquals(listOf(0, 2, 1, 3), selections) }
    }
    @Test fun attention_and_assistant_maintenance_states() {
        compose.setContent {
            DashaTheme {
                Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("运营维护", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        AttentionSummary(6, 2)
                        AssistantUpdatePanel(true, "正在通知 3 台助手更新…", {})
                        OperationFeedback("请前往设备详情核对助手版本与上报时间。")
                    }
                }
            }
        }
        compose.onNodeWithText("正在发送更新通知…").assertIsNotEnabled()
        screenshot("operations-states")
    }
    @Test fun account_operations_keep_callbacks_and_equal_columns() {
        val actions = mutableListOf<String>()
        compose.setContent { DashaTheme { AccountOperationsPanel(false, null,
            { actions.add("pause") }, { actions.add("production") }, { actions.add("sell") }, { actions.add("schedule") }) } }
        listOf("暂停", "制造", "出售", "定时暂停").forEach { compose.onNodeWithText(it).performClick() }
        compose.runOnIdle { assertEquals(listOf("pause", "production", "sell", "schedule"), actions) }
        val pause = compose.onNodeWithText("暂停").fetchSemanticsNode().boundsInRoot
        val production = compose.onNodeWithText("制造").fetchSemanticsNode().boundsInRoot
        assertEquals(pause.width, production.width, 1f)
    }
    @Test @Config(qualifiers = "w320dp-h740dp-mdpi")
    fun account_operations_busy_narrow_large_font() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                DashaTheme { Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.padding(20.dp)) { AccountOperationsPanel(true, "resume", {}, {}, {}, {}) }
                } }
            }
        }
        listOf("恢复", "制造", "出售", "定时暂停").forEach { compose.onNodeWithText(it).assertIsDisplayed().assertIsNotEnabled() }
        screenshot("account-operations-320")
    }
    @Test fun recognition_running_exposes_cancel_without_close() {
        var cancelled = 0
        var closed = 0
        compose.setContent { DashaTheme { RecognitionStatusDialog(true, RecognizeMode.AW, 3, 8, 2, 1, "示例账号", null,
            { cancelled++ }, { closed++ }) } }
        compose.onNodeWithText("已处理 3 / 8 个账号").assertExists()
        compose.onNodeWithText("完成").assertDoesNotExist()
        compose.onNodeWithText("取消识别").performClick()
        compose.runOnIdle { assertEquals(1, cancelled); assertEquals(0, closed) }
    }
    @Test fun recognition_error_remains_visible_and_can_close() {
        var closed = false
        compose.setContent { DashaTheme { RecognitionStatusDialog(false, RecognizeMode.LEVELS, 0, 0, 0, 0, "", "请检查识别服务配置", {}, { closed = true }) } }
        compose.onNodeWithText("请检查识别服务配置").assertExists()
        compose.onNodeWithText("完成").performClick()
        compose.runOnIdle { assertTrue(closed) }
    }
    @Test @Config(qualifiers = "w320dp-h740dp-mdpi")
    fun recognition_result_narrow_large_font() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                DashaTheme { Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("识别任务已结束", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        DashaCard { RecognitionProgressContent(false, RecognizeMode.BOTH, 8, 8, 6, 2, "", "部分截图无法读取，请稍后重试。") }
                    }
                } }
            }
        }
        compose.onNodeWithText("识别成功").assertIsDisplayed()
        screenshot("recognition-result-320")
    }

    @Test fun maintenance_expansion_keeps_update_as_an_explicit_action() {
        var updates = 0
        compose.setContent { DashaTheme { AssistantUpdatePanel(false, null, { updates++ }) } }
        compose.onNodeWithText("批量更新设备助手").assertDoesNotExist()
        compose.onNodeWithContentDescription("展开助手维护").performClick()
        compose.onNodeWithText("批量更新设备助手").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, updates) }
        compose.onNodeWithText("批量更新设备助手").performClick()
        compose.runOnIdle { assertEquals(1, updates) }
        compose.onNodeWithContentDescription("收起助手维护").performClick()
        compose.onNodeWithText("批量更新设备助手").assertDoesNotExist()
    }
    @Test @Config(qualifiers = "w320dp-h740dp-mdpi")
    fun detail_tabs_fit_narrow_screen_and_large_font() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                DashaTheme { Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
                    Column {
                        DashaTopBar("主力设备 01", "设备详情")
                        DetailSectionTabs(listOf("概览", "控制", "助手", "账号"), 2, {})
                    }
                } }
            }
        }
        listOf("概览", "控制", "助手", "账号").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText("助手").assertIsSelected()
        screenshot("detail-tabs-320")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val target = File("build/design-previews/$name.png")
        target.parentFile?.mkdirs()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
