package com.marzec.todo.cache

import kotlinx.coroutines.flow.Flow

interface Cache {

    suspend fun put(key: String, value: Any?)

    suspend fun <T> get(key: String): T?

    suspend fun <T> observe(key: String): Flow<T?>

    suspend fun toMap(): Map<String, Any?>
}

