package com.marzec.navigation

import com.marzec.cache.Cache
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ResultCache(private val memoryCache: Cache) {

    suspend fun <T> observe(
        requesterKey: String,
        resultKey: String
    ): Flow<T?> {
        val stringKey = createKey(requesterKey, resultKey)
        return memoryCache.observe<T>(stringKey)
    }

    suspend fun save(
        resultKey: String,
        value: Any?
    ) {
        allKeys().filter { it.resultKey == resultKey }
            .forEach {
                val stringKey = createKey(it.requesterKey, it.resultKey)
                memoryCache.put(stringKey, value)
            }
    }

    suspend fun remove(requesterKey: String) {
        allKeys().filter { it.requesterKey == requesterKey }.forEach {
            val stringKey = createKey(it.requesterKey, it.resultKey)
            memoryCache.remove(stringKey)
        }
    }

    suspend fun clean(resultKey: String) {
        allKeys().filter { it.resultKey == resultKey }.forEach {
            val stringKey = createKey(it.requesterKey, it.resultKey)
            memoryCache.put(stringKey, null)
        }
    }

    private suspend fun allKeys() =
        memoryCache.toMap().keys.map { Json.decodeFromString<ResultKey>(it) }

    private fun createKey(requesterKey: String, resultKey: String): String =
        Json.encodeToString(ResultKey(requesterKey, resultKey))
}

@Serializable
private data class ResultKey(val requesterKey: String, val resultKey: String)