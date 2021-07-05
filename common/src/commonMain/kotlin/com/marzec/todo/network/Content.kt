package com.marzec.todo.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
fun <T> asContentFlow(request: suspend () -> T): Flow<Content<T>> {
    return flow {
        emit(Content.Loading())
        try {
            emit(Content.Data(request()))
        } catch (e: Exception) {
            println(e.message)
            emit(Content.Error<T>(e))
        }
    }
}

fun <T, R> Content<T>.mapData(mapper: (T) -> R) = when (this) {
    is Content.Data -> try {
        Content.Data(mapper(this.data))
    } catch (e: Exception) {
        println(e.message)
        Content.Error(e)
    }
    is Content.Loading -> Content.Loading(this.data?.let(mapper))
    is Content.Error -> Content.Error(this.exception)
}

fun <T> Content<T>.ifData(action: Content.Data<T>.() -> Unit) = (this as? Content.Data)?.action()

suspend fun <T> Content<T>.ifDataSuspend(action: suspend Content.Data<T>.() -> Unit) =
    (this as? Content.Data)?.action()