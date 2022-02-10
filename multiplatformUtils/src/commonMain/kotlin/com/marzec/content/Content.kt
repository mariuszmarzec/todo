package com.marzec.content

import com.marzec.logger.Logger
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
    } catch (expected: Exception) {
        Logger.log("Content", expected.message.toString())
        Content.Error(expected)
    }
}
fun <T> asContentFlow(request: suspend () -> T): Flow<Content<T>> {
    return flow {
        emit(Content.Loading())
        try {
            emit(Content.Data(request()))
        } catch (expected: Exception) {
            Logger.log("Content", expected.message.toString())
            emit(Content.Error<T>(expected))
        }
    }
}

fun <T, R> Content<T>.mapData(mapper: (T) -> R) = when (this) {
    is Content.Data -> try {
        Content.Data(mapper(this.data))
    } catch (expected: Exception) {
        Content.Error(expected)
    }
    is Content.Loading -> Content.Loading(this.data?.let(mapper))
    is Content.Error -> Content.Error(this.exception)
}

suspend fun <T> Content<T>.ifDataSuspend(action: suspend Content.Data<T>.() -> Unit) =
    (this as? Content.Data)?.action()

fun <T> Content.Error<T>.getMessage(): String = exception.message.orEmpty()
