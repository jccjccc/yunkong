package com.dashabi.control.ui

import com.dashabi.control.data.Facilities
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/** 设施完成时间的解析与「已超时时长」计算。 */
object FacilityTimes {

    fun parse(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim()
        return runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
            ?: runCatching {
                val now = ZonedDateTime.now()
                val fmt = DateTimeFormatterBuilder()
                    .appendPattern("MM-dd HH:mm")
                    .parseDefaulting(ChronoField.YEAR, now.year.toLong())
                    .toFormatter()
                var value = LocalDateTime.parse(text, fmt).atZone(ZoneId.systemDefault())
                if (value.isBefore(now.minusDays(1))) value = value.plusYears(1)
                if (value.isAfter(now.plusDays(1))) value = value.minusYears(1)
                value.toInstant().toEpochMilli()
            }.getOrNull()
    }

    /** 四个设施里最长的已超时时长；没有超时返回 null。 */
    fun maxOverdue(facilities: Facilities?): Long? {
        if (facilities == null) return null
        val now = System.currentTimeMillis()
        return listOfNotNull(
            facilities.tech, facilities.workbench, facilities.pharmacy, facilities.armory
        ).mapNotNull { parse(it.finishAt) }
            .filter { it < now }
            .maxOfOrNull { now - it }
    }
}
