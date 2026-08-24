package com.marzec.preferences

import kotlinx.coroutines.flow.Flow

interface StateCache {
    suspend fun set(key: String, value: Any)
    suspend fun <T> get(key: String): T?
    suspend fun remove(key: String)
    suspend fun <T> observe(key: String): Flow<T?>
    suspend fun toMap(): Map<String, Any?>
}

class MemoryStateCache : StateCache {

    private val map = HashMap<String, Any>()

    override suspend fun set(key: String, value: Any) {
        map[key] = value
    }

    @Suppress("unchecked_cast")
    override suspend fun <T> get(key: String): T? = map[key] as? T

    override suspend fun remove(key: String) {
        map.remove(key)
    }

    override suspend fun <T> observe(key: String): Flow<T?> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(map[key] as? T)
            kotlinx.coroutines.delay(100)
        }
    }

    override suspend fun toMap(): Map<String, Any?> = map.toMap()
}