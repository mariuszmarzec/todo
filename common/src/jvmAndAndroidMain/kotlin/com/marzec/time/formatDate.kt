package com.marzec.time

import com.marzec.todo.Api
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual fun LocalDateTime.formatDate(): String = format(
    DateTimeFormatter.ofPattern(Api.DATE_FORMAT))
