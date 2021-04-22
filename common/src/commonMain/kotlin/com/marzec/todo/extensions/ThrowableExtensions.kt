package com.marzec.todo.extensions

fun Throwable.getMessage() = message.orEmpty()