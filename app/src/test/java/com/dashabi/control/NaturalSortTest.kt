package com.dashabi.control

import com.dashabi.control.ui.naturalCompare
import org.junit.Assert.assertEquals
import org.junit.Test

class NaturalSortTest {

    private fun sortNames(names: List<String>): List<String> =
        names.sortedWith { a, b -> naturalCompare(a, b) }

    @Test
    fun device_numbers_sort_by_value() {
        val input = listOf("设备10", "设备2", "设备1", "设备3")
        assertEquals(listOf("设备1", "设备2", "设备3", "设备10"), sortNames(input))
    }

    @Test
    fun handles_large_numbers_and_prefixes() {
        val input = listOf("手机A10", "手机A2", "手机A1", "手机B1")
        assertEquals(listOf("手机A1", "手机A2", "手机A10", "手机B1"), sortNames(input))
    }

    @Test
    fun handles_leading_zeros() {
        val input = listOf("设备010", "设备2", "设备001")
        assertEquals(listOf("设备001", "设备2", "设备010"), sortNames(input))
    }

    @Test
    fun pure_numbers() {
        val input = listOf("10", "9", "100", "1")
        assertEquals(listOf("1", "9", "10", "100"), sortNames(input))
    }
}
