package com.dashabi.control

import com.dashabi.control.data.Account
import com.dashabi.control.ui.matchesSearch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSearchTest {

    private val account = Account(
        accountCode = "3637660953",
        deviceCode = "c367cf86",
        deviceName = "设备1",
        loginType = "qq",
        gameId = "54918569818755412663",
        gameNick = "科音乔治yy",
        upstreamRemark = "4低分",
        websiteNote = "备用号",
        status = "normal"
    )

    @Test
    fun matches_nickname() {
        assertTrue(account.matchesSearch("科音"))
        assertTrue(account.matchesSearch("乔治yy"))
    }

    @Test
    fun matches_game_id_and_account_code() {
        assertTrue(account.matchesSearch("54918569"))
        assertTrue(account.matchesSearch("3637660953"))
    }

    @Test
    fun matches_remark_and_device() {
        assertTrue(account.matchesSearch("低分"))
        assertTrue(account.matchesSearch("设备1"))
        assertTrue(account.matchesSearch("备用号"))
    }

    @Test
    fun matches_ignores_case() {
        assertTrue(account.matchesSearch("QQ"))
        assertTrue(account.matchesSearch("qq"))
    }

    @Test
    fun empty_keyword_matches_everything() {
        assertTrue(account.matchesSearch(""))
        assertTrue(account.matchesSearch("   "))
    }

    @Test
    fun rejects_unknown_keyword() {
        assertFalse(account.matchesSearch("不存在"))
    }
}
