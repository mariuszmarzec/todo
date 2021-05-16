package com.marzec.todo.network

sealed class Content<T> {

    data class Data<T>(val data: T) : Content<T>()
    data class Loading<T>(val data: T? = null) : Content<T>()
    data class Error<T>(val exception: Throwable) : Content<T>()
}

suspend fun <T> asContent(request: suspend () -> T): Content<T> {
    return try {
        Content.Data(request())
    } catch (e: Exception) {
        println(e.message)
        Content.Error(e)
    }
}

fun <T> Content<T>.ifData(action: Content.Data<T>.() -> Unit) = (this as? Content.Data)?.action()

suspend fun <T> Content<T>.ifDataSuspend(action: suspend Content.Data<T>.() -> Unit) = (this as? Content.Data)?.action()