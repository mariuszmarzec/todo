package com.marzec.mvi

import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.asInstanceAndReturnOther
import com.marzec.todo.network.Content

sealed class State<T>(open val data: T?) {

    data class Data<T>(override val data: T) : State<T>(data)
    data class Loading<T>(override val data: T? = null) : State<T>(data)
    data class Error<T>(override val data: T? = null, val message: String) : State<T>(data)

    inline fun asData(action: T.() -> Unit) =
        asInstance<Data<T>> {
            action(data)
        }

    inline fun <R> ifDataAvailable(
        blockOnLoading: Boolean = true,
        action: T.() -> R
    ) = data?.takeIf { !blockOnLoading || this !is Loading }?.let(action)
}

fun <T, R> State<T>.reduceContentNoChanges(result: Content<R>): State<T> =
    reduceContent(result) { this }

fun <T, R> State<T>.reduceContent(
    result: Content<R>,
    reducer: State<T>.(Content.Data<R>) -> State<T>
): State<T> =
    when (result) {
        is Content.Data -> this.reducer(result)
        is Content.Loading -> State.Loading(data)
        is Content.Error -> State.Error(
            data,
            result.exception.message.orEmpty()
        )
    }

fun <T> State<T>.reduceData(
    reducer: T.() -> T
): State<T> =
    when (this) {
        is State.Data -> State.Data(this.data.reducer())
        is State.Loading -> copy(this.data?.reducer())
        is State.Error -> copy(this.data?.reducer())
    }

fun <T, R> State<T>.reduceDataWithContent(
    result: Content<R>,
    defaultData: T,
    reducer: T.(Content.Data<R>) -> T
): State<T> =
    when (result) {
        is Content.Data -> {
            State.Data((this.data ?: defaultData).reducer(result))
        }
        is Content.Loading -> State.Loading(data)
        is Content.Error -> State.Error(
            data,
            result.exception.message.orEmpty()
        )
    }

fun <T, R> State<T>.reduceDataWithContent(
    result: Content<R>,
    reducer: T?.(R) -> T
): State<T> =
    when (result) {
        is Content.Data -> State.Data((this.data).reducer(result.data))
        is Content.Loading -> State.Loading(data)
        is Content.Error -> State.Error(data, result.exception.message.orEmpty())
    }

fun <T, R> State<T>.mapData(mapper: (T) -> R) = when (this) {
    is State.Data -> State.Data(mapper(this.data))
    is State.Loading -> State.Loading(this.data?.let(mapper))
    is State.Error -> State.Error(this.data?.let(mapper), this.message)
}

fun <T, R> State<T>.reduceContentAsSideAction(
    result: Content<R>
): State<T> =
    when (result) {
        is Content.Loading -> State.Loading(data)
        else -> {
            val data = data
            if (data != null) {
                State.Data(data)
            } else {
                State.Error(data, message = "No data available")
            }
        }
    }
