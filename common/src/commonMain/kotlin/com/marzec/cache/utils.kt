package com.marzec.cache

import com.marzec.content.Content
import com.marzec.content.asContentFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
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
    networkCall: suspend () -> Content<T>
): Flow<Content<T>> =
    withContext(dispatcher) {
        val cached = memoryCache.observe<T>(key).firstOrNull()
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
                    memoryCache.put(key, callResult.data)
                }
            },
            memoryCache.observe<T>(key).filterNotNull().map { Content.Data(it) as Content<T> }
        )
    }.flowOn(dispatcher)

fun asContentWithListUpdate(
    dispatcher: CoroutineDispatcher,
    request: suspend () -> Unit,
    refreshCallback: suspend () -> Unit
) =
    asContentFlow(request)
        .onEach {
            if (it is Content.Data) {
                refreshCallback()
            }
        }.flowOn(dispatcher)
