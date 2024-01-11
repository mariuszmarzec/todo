package com.marzec.cache

import com.marzec.content.Content
import com.marzec.content.asContentFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

suspend fun <T : Any> cacheCall(
    key: String,
    dispatcher: CoroutineDispatcher,
    memoryCache: Cache,
    networkCall: suspend () -> Content<T>,
    getCached: suspend () -> T? = { memoryCache.get<T>(key) },
    observeCached: suspend () -> Flow<T?> = { memoryCache.observe<T>(key) },
    cacheUpdate: suspend (Content.Data<T>) -> Unit = { memoryCache.put(key, it.data) }
): Flow<Content<T>> =
    withContext(dispatcher) {
        val cached = getCached()
        val initial: Content<T> = if (cached != null) {
            Content.Data(cached)
        } else {
            Content.Loading()
        }
        merge(
            flow {
                emit(initial)
                val callResult = networkCall()
                if (callResult is Content.Error) {
                    emit(callResult)
                } else if (callResult is Content.Data) {
                    cacheUpdate(callResult)
                }
            },
            observeCached().filterNotNull().map { Content.Data(it) as Content<T> }
        )
    }.flowOn(dispatcher)

fun asContentWithListUpdate(
    dispatcher: CoroutineDispatcher,
    refreshCallback: suspend () -> Unit,
    request: suspend () -> Unit
) =
    asContentFlow(request)
        .onEach {
            if (it is Content.Data) {
                refreshCallback()
            }
        }.flowOn(dispatcher)
