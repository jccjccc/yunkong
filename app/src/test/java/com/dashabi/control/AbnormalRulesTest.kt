package com.dashabi.control

import com.dashabi.control.data.Account
import com.dashabi.control.data.Facilities
import com.dashabi.control.data.Facility
import com.dashabi.control.ui.vm.AbnormalAccount
import com.dashabi.control.ui.vm.AbnormalViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AbnormalRulesTest {

    private fun at(minutesFromNow: Long): String =
        LocalDateTime.now().plusMinutes(minutesFromNow)
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))

    private fun facility(finishAt: String = at(120)) = Facility(level = 1, finishAt = finishAt)

    private fun account(
        haf: String? = "2,500K",
        status: String? = "normal",
        rawStatus: String? = null,
        missingArmory: Boolean = false,
        techFinishAt: String = at(120),
        workbenchFinishAt: String = at(120),
        paused: Boolean = false
    ): Account = Account(
        accountCode = "10001",
        hafCoin = haf,
        status = status,
        rawStatus = rawStatus,
        paused = paused,
        facilities = Facilities(
            tech = facility(techFinishAt),
            workbench = facility(workbenchFinishAt),
            pharmacy = facility(),
            armory = if (missingArmory) Facility(level = 1, finishAt = null) else facility()
        )
    )

    @Test
    fun normal_account_has_no_reason() {
        assertEquals(emptyList<String>(), AbnormalViewModel.abnormalReasons(account()))
    }

    @Test
    fun low_haf_coin_is_abnormal() {
        assertTrue(AbnormalViewModel.abnormalReasons(account(haf = "629,402")).contains("哈夫币不足"))
    }

    @Test
    fun invalid_haf_coin_is_abnormal() {
        assertTrue(AbnormalViewModel.abnormalReasons(account(haf = "国")).contains("哈夫币数据异常"))
    }

    @Test
    fun missing_facility_time_is_abnormal() {
        assertTrue(
            AbnormalViewModel.abnormalReasons(account(missingArmory = true)).contains("防具台 无完成时间")
        )
    }

    @Test
    fun cloud_abnormal_status_is_included() {
        assertTrue(AbnormalViewModel.abnormalReasons(account(status = "abnormal")).contains("云端异常"))
    }

    private fun abnormal(code: String) = AbnormalAccount(
        account = Account(accountCode = code),
        reasons = listOf("哈夫币不足"),
        hafCoin = 1.0
    )

    @Test
    fun first_load_only_establishes_baseline() {
        assertEquals(
            emptyList<AbnormalAccount>(),
            AbnormalViewModel.newAbnormal(null, listOf(abnormal("a")))
        )
    }

    @Test
    fun only_new_codes_are_reported() {
        val added = AbnormalViewModel.newAbnormal(
            previous = setOf("a", "c"),
            current = listOf(abnormal("a"), abnormal("b"))
        )
        assertEquals(listOf("b"), added.map { it.account.accountCode })
    }

    @Test
    fun verification_raw_status_is_abnormal() {
        assertTrue(AbnormalViewModel.abnormalReasons(account(rawStatus = "蓝脸")).contains("需要验证"))
        assertTrue(AbnormalViewModel.abnormalReasons(account(rawStatus = "需要验证")).contains("需要验证"))
        assertTrue(AbnormalViewModel.abnormalReasons(account(rawStatus = "人脸验证")).contains("需要验证"))
    }

    @Test
    fun overdue_facility_is_abnormal() {
        assertTrue(
            AbnormalViewModel.abnormalReasons(account(techFinishAt = at(-20))).contains("技术中心 超时")
        )
    }

    @Test
    fun paused_account_facility_timeout_is_not_abnormal() {
        assertEquals(
            emptyList<String>(),
            AbnormalViewModel.abnormalReasons(
                account(status = "paused", paused = true, techFinishAt = at(-30))
            )
        )
    }

    @Test
    fun facility_finished_within_ten_minutes_is_not_abnormal() {
        assertEquals(
            emptyList<String>(),
            AbnormalViewModel.abnormalReasons(account(techFinishAt = at(-5)))
        )
    }

    @Test
    fun frozen_raw_status_is_abnormal() {
        assertTrue(AbnormalViewModel.abnormalReasons(account(rawStatus = "账号冻结")).contains("账号冻结"))
        assertTrue(AbnormalViewModel.abnormalReasons(account(status = "frozen")).contains("账号冻结"))
    }

    @Test
    fun workbench_timeout_is_shown_first_and_haf_last() {
        val reasons = AbnormalViewModel.abnormalReasons(
            account(
                haf = "100,000",
                techFinishAt = at(120),
                workbenchFinishAt = at(-20)
            )
        )
        assertEquals("工作台 超时", reasons.first())
        assertEquals("哈夫币不足", reasons.last())
    }

    @Test
    fun list_priority_puts_workbench_first_and_haf_only_last() {
        val workbench = AbnormalAccount(
            account = Account(accountCode = "a"),
            reasons = listOf("技术中心 超时", "哈夫币不足"),
            hafCoin = 1.0
        )
        val other = AbnormalAccount(
            account = Account(accountCode = "b"),
            reasons = listOf("账号冻结"),
            hafCoin = 2.0
        )
        val hafOnly = AbnormalAccount(
            account = Account(accountCode = "c"),
            reasons = listOf("哈夫币不足"),
            hafCoin = 3.0
        )
        val sorted = listOf(other, hafOnly, workbench).sortedWith(
            compareBy<AbnormalAccount>(
                {
                    when {
                        it.reasons.any { r -> r.endsWith("超时") } -> 0
                        it.reasons.all { r -> r.startsWith("哈夫币") } -> 2
                        else -> 1
                    }
                },
                { it.hafCoin ?: 0.0 }
            )
        )
        assertEquals(listOf("a", "b", "c"), sorted.map { it.account.accountCode })
    }
}