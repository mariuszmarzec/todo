package com.marzec.time

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual fun LocalDateTime.formatDate(dateFormat: String): String = format(
    DateTimeFormatter.ofPattern(dateFormat))
