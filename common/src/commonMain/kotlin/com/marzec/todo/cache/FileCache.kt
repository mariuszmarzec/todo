package com.marzec.todo.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

interface FileCache {

    suspend fun put(key: String, value: Any?)

    suspend fun get(key: String): JsonElement?

    suspend fun observe(key: String): Flow<JsonElement?>
}

suspend inline fun <reified T> FileCache.getTyped(key: String): T? = get(key)?.let {
    Json.Default.decodeFromJsonElement(it)
}

suspend inline fun <reified T> FileCache.observeTyped(key: String): Flow<T?> {
    return observe(key).map { jsonElement ->
        jsonElement
            ?.let { it1 -> Json.Default.decodeFromJsonElement(it1) }
    }
}
