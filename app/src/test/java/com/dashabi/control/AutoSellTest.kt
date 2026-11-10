package com.dashabi.control

import com.dashabi.control.data.Account
import com.dashabi.control.data.DeviceStatus
import com.dashabi.control.ui.vm.AutoSellViewModel
import com.dashabi.control.ui.vm.deviceAbnormalReasons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AutoSellTest {

    private fun account(haf: String?, device: String? = "device-1") = Account(
        accountCode = "10001",
        deviceCode = device,
        hafCoin = haf
    )

    @Test
    fun low_haf_coin_is_sell_target() {
        assertTrue(AutoSellViewModel.isAutoSellTarget(account("629,402")))
        assertTrue(AutoSellViewModel.isAutoSellTarget(account("999,999")))
        assertTrue(AutoSellViewModel.isAutoSellTarget(account("16K")))
    }

    @Test
    fun threshold_and_unknown_are_not_targets() {
        assertFalse(AutoSellViewModel.isAutoSellTarget(account("1,000,000")))
        assertFalse(AutoSellViewModel.isAutoSellTarget(account("2.5M")))
        assertFalse(AutoSellViewModel.isAutoSellTarget(account(null)))
        assertFalse(AutoSellViewModel.isAutoSellTarget(account("国")))
    }

    @Test
    fun account_without_device_is_not_target() {
        assertFalse(AutoSellViewModel.isAutoSellTarget(account("100,000", device = null)))
        assertFalse(AutoSellViewModel.isAutoSellTarget(account("100,000", device = "")))
    }

    @Test
    fun slot_aligns_to_even_local_hour() {
        val at = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 29, 15, 42, 10)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val slot = Calendar.getInstance().apply {
            timeInMillis = AutoSellViewModel.slotStart(at)
        }

        assertEquals(14, slot.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, slot.get(Calendar.MINUTE))
        assertEquals(0, slot.get(Calendar.SECOND))
    }

    @Test
    fun slot_handles_midnight_and_last_hours() {
        fun hourOf(hour: Int): Int {
            val at = Calendar.getInstance().apply {
                set(2026, Calendar.OCTOBER, 29, hour, 5, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            return Calendar.getInstance().apply {
                timeInMillis = AutoSellViewModel.slotStart(at)
            }.get(Calendar.HOUR_OF_DAY)
        }
        assertEquals(0, hourOf(0))
        assertEquals(0, hourOf(1))
        assertEquals(22, hourOf(23))
    }

    @Test
    fun paused_account_is_not_target() {
        assertFalse(AutoSellViewModel.isAutoSellTarget(account("100,000").copy(paused = true)))
        assertFalse(AutoSellViewModel.isAutoSellTarget(account("100,000").copy(status = "paused")))
    }

    @Test
    fun device_abnormal_reasons() {
        assertTrue(deviceAbnormalReasons(DeviceStatus(code = "running", timerEnabled = false)).contains("定时已关闭"))
        assertTrue(deviceAbnormalReasons(DeviceStatus(code = "stopped")).contains("已停止"))
        assertTrue(deviceAbnormalReasons(DeviceStatus(code = "accessibility_offline")).contains("无障碍关闭"))
        assertTrue(deviceAbnormalReasons(DeviceStatus(code = "running", timerEnabled = true)).isEmpty())
    }
}