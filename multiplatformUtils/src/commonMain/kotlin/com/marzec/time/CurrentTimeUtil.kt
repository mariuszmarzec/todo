package com.marzec.time

import java.time.Clock
import java.time.LocalDate
import kotlinx.datetime.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.datetime.toKotlinLocalDateTime

const val DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

object CurrentTimeUtil {

    internal var clock = Clock.systemDefaultZone()

    fun setOtherTime(day: Int, month: Int, year: Int) {
        val instant = LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant()
        clock = Clock.fixed(instant, ZoneId.systemDefault())
    }
}

fun currentTime(): LocalDateTime = java.time.LocalDateTime.now(CurrentTimeUtil.clock).toKotlinLocalDateTime()

fun currentMillis(): Long =
    java.time.LocalDateTime.now(CurrentTimeUtil.clock).toInstant(OffsetDateTime.now().offset).toEpochMilli()

expect fun LocalDateTime.formatDate(dateFormat: String = DEFAULT_DATE_FORMAT): String
