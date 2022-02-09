package com.marzec.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

interface FileCache {

    suspend fun <T: Any> put(key: String, value: T?, serializer: KSerializer<T>)

    suspend fun <T: Any> get(key: String, serializer: KSerializer<T>): T?

    suspend fun <T: Any> observe(key: String, serializer: KSerializer<T>): Flow<T?>
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T: Any> FileCache.putTyped(key: String, value: T?) {
    put(key, value, T::class.serializer())
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T: Any> FileCache.getTyped(key: String): T? {
    return get(key, T::class.serializer())
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T: Any> FileCache.observeTyped(key: String): Flow<T?> {
    return observe(key, T::class.serializer())
}
