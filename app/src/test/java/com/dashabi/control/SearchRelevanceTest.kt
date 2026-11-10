package com.dashabi.control

import com.dashabi.control.data.Account
import com.dashabi.control.ui.searchRelevance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRelevanceTest {

    private fun account(
        code: String = "3905769804",
        nick: String = "真露白凝冰",
        remark: String? = null,
        gameId: String = "186836064501833285909",
        device: String = "设备1"
    ) = Account(
        accountCode = code,
        gameNick = nick,
        upstreamRemark = remark,
        gameId = gameId,
        deviceName = device
    )

    @Test
    fun exact_account_code_ranks_highest() {
        val target = account(code = "3905", nick = "x")
        assertEquals(110, target.searchRelevance("3905"))
    }

    @Test
    fun code_beats_nickname_and_remark() {
        val a = account(code = "3905769804", nick = "3905玩家", remark = "3905备注")
        assertEquals(true, a.searchRelevance("3905") >= 90)
        assertTrue(a.searchRelevance("3905") > a.searchRelevance("玩家").let { 0 })
    }

    @Test
    fun remark_ranks_above_account_code() {
        val a = account(code = "3905", remark = "x3905y")
        assertTrue(a.searchRelevance("3905") > account(code = "3905").searchRelevance("3905"))
    }

    @Test
    fun remark_beats_device_name() {
        val a = account(remark = "px8608低分", device = "设备8608")
        assertTrue(a.searchRelevance("8608") > 22)
    }

    @Test
    fun no_match_scores_zero() {
        assertEquals(0, account().searchRelevance("不存在的关键词"))
        assertEquals(0, account().searchRelevance(""))
    }

    @Test
    fun game_id_matches() {
        val a = account(gameId = "186836064501833285909")
        assertTrue(a.searchRelevance("1868360") >= 86)
    }
}
