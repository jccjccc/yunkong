package com.dashabi.control.data.remote

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal const val VISION_PROMPT =
    "这张图片是《三角洲行动》游戏里的「特勤处」页面截图。请找到设施列表中的「训练中心」和「靶场」，" +
        "读出它们后面的等级数字（例如 Lv.6 就是 6）。" +
        "只输出 JSON，不要任何解释，格式：" +
        "{\"training_center\": 数字或 null, \"shooting_range\": 数字或 null}"

internal const val BULLETS_PROMPT =
    "这张图片是《三角洲行动》游戏里的「仓库」页面截图，右侧是物品网格。" +
        "请只找弹药格子里名称以「338」开头的那个（显示为 338 AP 或 .338 AP，可能分两行写成 338 和 AP，玩家简称 AW）。" +
        "数量数字显示在这个弹药格子自己的底部（例如 16），不是旁边大格子的数字。" +
        "重要：\n" +
        "1. 只取 338 AP 那个小格子自己的数量，不要把邻近背包/胸挂/头盔等装备格子下方的数字（例如 GTO 重型战术包下面的 45，那是负重/属性）当成子弹数。\n" +
        "2. 不要取 5.45x39、300 BLK、12 Gauge、9x19 等其它弹药的数量。\n" +
        "3. 有多格 338 AP 才把各自数量相加。\n" +
        "请逐格列出 338 AP，并把容易混淆的装备数字放在 ignored 里（不要计入）：\n" +
        "{\"stacks\":[{\"name\":\"338 AP\",\"count\":16}],\"ignored\":[{\"name\":\"GTO 重型战术包\",\"number\":45}]}\n" +
        "没有 338 AP 时输出 {\"stacks\":[]}。只输出 JSON，不要解释。"

/**
 * 构造 DeepSeek Chat Completions 的视觉请求体。
 * 关闭思考模式以降低延迟与费用，并要求返回 JSON。
 */
fun buildVisionPayload(
    model: String,
    base64Jpeg: String,
    prompt: String = VISION_PROMPT
): String = buildJsonObject {
    put("model", model)
    put("temperature", 0)
    put("max_tokens", 200)
    putJsonObject("thinking") { put("type", "disabled") }
    putJsonObject("response_format") { put("type", "json_object") }
    putJsonArray("messages") {
        add(
            buildJsonObject {
                put("role", "user")
                putJsonArray("content") {
                    add(
                        buildJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                    )
                    add(
                        buildJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", "data:image/jpeg;base64,$base64Jpeg")
                                put("detail", "high")
                            }
                        }
                    )
                }
            }
        )
    }
}.toString()
