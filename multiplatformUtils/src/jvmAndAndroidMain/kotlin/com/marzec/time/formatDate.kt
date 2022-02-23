package com.marzec.time

import kotlinx.datetime.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.datetime.toJavaLocalDateTime

actual fun LocalDateTime.formatDate(dateFormat: String): String = toJavaLocalDateTime().format(
    DateTimeFormatter.ofPattern(dateFormat))
