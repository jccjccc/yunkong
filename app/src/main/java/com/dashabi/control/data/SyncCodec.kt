package com.dashabi.control.data

private val markerRegex = Regex("\\[DSYNC\\](.*?)\\[/DSYNC\\]", RegexOption.DOT_MATCHES_ALL)

data class SyncPayload(
    val training: Int? = null,
    val range: Int? = null,
    val aw: Int? = null,
    val credit: Int? = null
) {
    val isEmpty: Boolean get() = training == null && range == null && aw == null && credit == null
}

fun encodeSyncMarker(payload: SyncPayload): String {
    val parts = buildList {
        payload.training?.let { add("t=$it") }
        payload.range?.let { add("r=$it") }
        payload.aw?.let { add("aw=$it") }
        payload.credit?.let { add("c=$it") }
    }
    return "[DSYNC]" + parts.joinToString(";") + "[/DSYNC]"
}

fun parseSyncMarker(note: String?): SyncPayload? {
    val body = markerRegex.find(note ?: "")?.groupValues?.getOrNull(1) ?: return null
    var training: Int? = null
    var range: Int? = null
    var aw: Int? = null
    var credit: Int? = null
    body.split(';').forEach { part ->
        val kv = part.split('=', limit = 2)
        if (kv.size != 2) return@forEach
        val value = kv[1].trim().toIntOrNull() ?: return@forEach
        when (kv[0].trim()) {
            "t" -> training = value
            "r" -> range = value
            "aw" -> aw = value
            "c" -> credit = value
        }
    }
    return SyncPayload(training, range, aw, credit)
}

fun stripSyncMarker(note: String?): String =
    markerRegex.replace(note ?: "", "").trim()
