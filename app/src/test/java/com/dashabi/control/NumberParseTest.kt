package com.dashabi.control

import com.dashabi.control.ui.formatHafCoinWan
import com.dashabi.control.ui.parseCompactNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberParseTest {

    @Test
    fun parses_plain_numbers() {
        assertEquals(850.0, parseCompactNumber("850")!!, 0.001)
        assertEquals(54.0, parseCompactNumber("54")!!, 0.001)
    }

    @Test
    fun k_means_thousand_with_thousands_grouping() {
        // haf_coin 以千为单位，17.050K = 17,050 千 = 17,050,000
        assertEquals(17_050_000.0, parseCompactNumber("17.050K")!!, 0.001)
        assertEquals(21_390_000.0, parseCompactNumber("21,390K")!!, 0.001)
        assertEquals(41_223_000.0, parseCompactNumber("41,223K")!!, 0.001)
        assertEquals(1_002_000.0, parseCompactNumber("1,002K")!!, 0.001)
        assertEquals(9_930_000.0, parseCompactNumber("9,930K")!!, 0.001)
        assertEquals(3_224_000.0, parseCompactNumber("3.224K")!!, 0.001)
    }

    @Test
    fun k_decimal_when_not_grouped_by_three() {
        // 不是三位一组时按小数处理，例如 1.2K = 1,200
        assertEquals(1_200.0, parseCompactNumber("1.2K")!!, 0.001)
        assertEquals(12_340.0, parseCompactNumber("12.34K")!!, 0.001)
    }

    @Test
    fun handles_leading_spaces_and_zeros() {
        assertEquals(2_001_000.0, parseCompactNumber(" 2.001K")!!, 0.001)
        assertEquals(2_956_000.0, parseCompactNumber("02.956K")!!, 0.001)
        assertEquals(21_681_000.0, parseCompactNumber("0 21,681K")!!, 0.001)
    }

    @Test
    fun comma_is_thousands_separator_without_suffix() {
        assertEquals(540_618.0, parseCompactNumber("540,618")!!, 0.001)
        assertEquals(629_402.0, parseCompactNumber("629,402")!!, 0.001)
        assertEquals(1_002.0, parseCompactNumber("1,002")!!, 0.001)
    }

    @Test
    fun parses_millions_and_billions() {
        assertEquals(31_500_000.0, parseCompactNumber("31.5M")!!, 0.001)
        assertEquals(1_200_000_000.0, parseCompactNumber("1.2B")!!, 0.001)
    }

    @Test
    fun multiple_separators_are_thousands() {
        assertEquals(1_234_567.0, parseCompactNumber("1,234,567")!!, 0.001)
        assertEquals(1_234_567.0, parseCompactNumber("1.234.567")!!, 0.001)
        assertEquals(1_234.56, parseCompactNumber("1.234,56")!!, 0.001)
        assertEquals(1_234.56, parseCompactNumber("1,234.56")!!, 0.001)
    }

    @Test
    fun formats_haf_coin_in_wan() {
        assertEquals("4122.3W", formatHafCoinWan("41,223K"))
        assertEquals("2832.9W", formatHafCoinWan("28.329K"))
        assertEquals("54.1W", formatHafCoinWan("540,618"))
        assertEquals("100.2W", formatHafCoinWan("1,002K"))
        assertEquals("—", formatHafCoinWan(null))
    }

    @Test
    fun returns_null_for_invalid_values() {
        assertNull(parseCompactNumber(null))
        assertNull(parseCompactNumber(""))
        assertNull(parseCompactNumber("—"))
        assertNull(parseCompactNumber("abc"))
    }

    @Test
    fun plain_dot_as_thousands() {
        assertEquals(863506.0, parseCompactNumber("863.506")!!, 0.0)
        assertEquals(17050.0, parseCompactNumber("17.050")!!, 0.0)
        assertEquals("86.4W", formatHafCoinWan("863.506"))
    }
}