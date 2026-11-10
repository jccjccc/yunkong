package com.dashabi.control

import com.dashabi.control.data.remote.BULLETS_PROMPT
import com.dashabi.control.data.remote.buildVisionPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionPayloadTest {

    @Test
    fun builds_deepseek_vision_request() {
        val payload = buildVisionPayload("deepseek-flash", "QUJD")
        val root = Json.parseToJsonElement(payload).jsonObject

        assertEquals("deepseek-flash", root["model"]!!.jsonPrimitive.content)
        assertEquals("disabled", root["thinking"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("json_object", root["response_format"]!!.jsonObject["type"]!!.jsonPrimitive.content)

        val content = root["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray
        assertEquals("text", content[0].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("image_url", content[1].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(
            "data:image/jpeg;base64,QUJD",
            content[1].jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content
        )
        assertEquals(
            "high",
            content[1].jsonObject["image_url"]!!.jsonObject["detail"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun builds_bullets_request_with_custom_prompt() {
        val payload = buildVisionPayload("deepseek-flash", "QUJD", BULLETS_PROMPT)
        val root = Json.parseToJsonElement(payload).jsonObject
        val content = root["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonArray
        val prompt = content[0].jsonObject["text"]!!.jsonPrimitive.content
        assertTrue(prompt.contains(".338"))
        assertTrue(prompt.contains("stacks"))
        assertTrue(prompt.contains("ignored"))
    }
}