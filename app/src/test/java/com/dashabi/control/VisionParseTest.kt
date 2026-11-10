package com.dashabi.control

import com.dashabi.control.data.parseVisionBullets
import com.dashabi.control.data.parseVisionLevels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VisionParseTest {

    @Test
    fun parses_plain_json() {
        val levels = parseVisionLevels("""{"training_center":6,"shooting_range":4}""")!!
        assertEquals(6, levels.training)
        assertEquals(4, levels.range)
    }

    @Test
    fun parses_fenced_json() {
        val content = """
            ```json
            {"training_center": 7, "shooting_range": 5}
            ```
        """.trimIndent()
        val levels = parseVisionLevels(content)!!
        assertEquals(7, levels.training)
        assertEquals(5, levels.range)
    }

    @Test
    fun parses_chinese_text() {
        val levels = parseVisionLevels("识别结果：训练中心：Lv.6，靶场：Lv.4")!!
        assertEquals(6, levels.training)
        assertEquals(4, levels.range)
    }

    @Test
    fun parses_string_level_values() {
        val levels = parseVisionLevels("""{"training_center":"Lv.6","shooting_range":null}""")!!
        assertEquals(6, levels.training)
        assertNull(levels.range)
    }

    @Test
    fun parses_partial_result() {
        val levels = parseVisionLevels("""{"shooting_range":5}""")!!
        assertNull(levels.training)
        assertEquals(5, levels.range)
    }

    @Test
    fun rejects_unrelated_content() {
        assertNull(parseVisionLevels("我看不清这张图片"))
        assertNull(parseVisionLevels(""))
        assertNull(parseVisionLevels(null))
    }

    @Test
    fun parses_aw_json() {
        assertEquals(123, parseVisionBullets("""{"aw_bullets":123}"""))
        assertEquals(75, parseVisionBullets("""{"aw":"75发"}"""))
        assertEquals(0, parseVisionBullets("""{"aw_bullets":0}"""))
    }

    @Test
    fun parses_aw_fenced_and_text() {
        val fenced = "```json\n{\"aw_bullets\": 42}\n```"
        assertEquals(42, parseVisionBullets(fenced))
        assertEquals(1234, parseVisionBullets("AW子弹：1,234发"))
        assertEquals(88, parseVisionBullets("仓库里 .338 AP 子弹数量为 88 发"))
    }

    @Test
    fun rejects_unclear_aw_content() {
        assertNull(parseVisionBullets("看不清有多少子弹"))
        assertNull(parseVisionBullets(""))
        assertNull(parseVisionBullets(null))
    }

    @Test
    fun sums_only_338_ammo_stacks() {
        val withBag = """{"stacks":[{"name":"338 AP","count":16}],"ignored":[{"name":"GTO 重型战术包","number":45}]}"""
        assertEquals(16, parseVisionBullets(withBag))

        val twoStacks = """{"stacks":[{"name":"338 AP","count":10},{"name":".338 AP","count":6}]}"""
        assertEquals(16, parseVisionBullets(twoStacks))

        assertEquals(0, parseVisionBullets("""{"stacks":[{"name":"5.45x39","count":60}]}"""))
        assertEquals(0, parseVisionBullets("""{"stacks":[]}"""))
    }
}