package com.marzec.todo.extensions

import com.marzec.todo.network.Content

fun <T> Content.Error<T>.getMessage() = exception.getMessage()