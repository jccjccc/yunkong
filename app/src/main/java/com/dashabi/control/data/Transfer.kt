package com.dashabi.control.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TransferStamina(
    val training: Int? = null,
    val range: Int? = null,
    val aw: Int? = null,
    val updatedAt: Long = 0
)

@Serializable
data class TransferCredit(
    val score: Int? = null,
    val nickname: String? = null,
    val updatedAt: Long = 0
)

@Serializable
data class TransferData(
    val version: Int = 1,
    val stamina: Map<String, TransferStamina> = emptyMap(),
    val credit: Map<String, TransferCredit> = emptyMap()
)

private val transferJson = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

fun buildTransferData(levels: Map<String, StaminaLevels>, credits: Map<String, CreditEntry>): String {
    val data = TransferData(
        stamina = levels.mapValues { (_, v) ->
            TransferStamina(v.training, v.range, v.awBullets, v.updatedAt)
        },
        credit = credits.mapValues { (_, v) ->
            TransferCredit(v.score, v.nickname, v.updatedAt)
        }
    )
    return transferJson.encodeToString(TransferData.serializer(), data)
}

fun parseTransferData(text: String): TransferData =
    transferJson.decodeFromString(TransferData.serializer(), text)
