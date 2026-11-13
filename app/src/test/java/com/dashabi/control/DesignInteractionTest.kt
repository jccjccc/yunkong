package com.dashabi.control

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dashabi.control.data.Account
import com.dashabi.control.ui.screens.AccountBalanceSummary
import com.dashabi.control.ui.screens.AccountCard
import com.dashabi.control.ui.screens.SearchBar
import com.dashabi.control.ui.theme.DashaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DesignInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun advanced_filters_expand_and_collapse() {
        compose.setContent {
            DashaTheme {
                com.dashabi.control.ui.screens.AccountListControls(false, {}, {},
                    advancedFilters = { androidx.compose.material3.Text("信用筛选测试") }, sort = {})
            }
        }
        compose.onNodeWithText("信用筛选测试").assertDoesNotExist()
        compose.onNodeWithText("更多筛选").performClick()
        compose.onNodeWithText("信用筛选测试").assertIsDisplayed()
        compose.onNodeWithText("收起筛选").performClick()
        compose.onNodeWithText("信用筛选测试").assertDoesNotExist()
    }

    @Test fun account_actions_remain_distinct_from_opening_the_card() {
        var opened = 0
        var paused = 0
        var sold = 0
        compose.setContent {
            DashaTheme {
                AccountCard(Account(accountCode = "offline", gameNick = "示例账号", status = "normal"),
                    levels = null, creditScore = null, gameName = null,
                    onOpen = { opened++ }, onTogglePause = { paused++ }, onSell = { sold++ })
            }
        }
        compose.onNodeWithText("暂停").performClick()
        compose.onNodeWithText("出售").performClick()
        compose.runOnIdle {
            assertEquals(1, paused)
            assertEquals(1, sold)
            assertEquals(0, opened)
        }
        compose.onNodeWithText("详情").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun balance_keeps_peak_comparison_action() {
        var opened = 0
        compose.setContent { DashaTheme { AccountBalanceSummary("120W", 3, { opened++ }) } }
        compose.onNodeWithText("查看昨日峰值对比").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun search_accepts_input_and_keeps_clear_action() {
        var value = ""
        var cleared = false
        compose.setContent { DashaTheme { SearchBar("abc", { value = it }, { cleared = true }, {}, {}) } }
        compose.onNode(hasSetTextAction()).performTextReplacement("测试账号")
        compose.runOnIdle { assertEquals("测试账号", value) }
        compose.onNodeWithContentDescription("清空").performClick()
        compose.runOnIdle { assertEquals(true, cleared) }
    }
}
