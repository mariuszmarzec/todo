package com.marzec.todo.network

sealed class Content<T> {

    data class Data<T>(val data: T) : Content<T>()
    data class Error<T>(val exception: Throwable) : Content<T>()
}

suspend fun <T> asContent(request: suspend () -> T): Content<T> {
    return try {
        Content.Data(request())
    } catch (e: Exception) {
        Content.Error(e)
    }
}