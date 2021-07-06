package com.marzec.todo.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MemoryCache : Cache {

    private val cache = ArrayList<Pair<String, MutableStateFlow<Any?>>>()

    override suspend fun put(key: String, value: Any?) {
        if (cache.any { it.first == key }) {
            cache.first { it.first == key }.second.emit(value)
        } else {
            cache.add(0, key to MutableStateFlow(value))
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> get(key: String): T? {
        return cache.firstOrNull { it.first == key }?.second?.value as? T
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> observe(key: String): Flow<T?> {
        val state = cache.firstOrNull { it.first == key } ?: put(
            key,
            null
        ).let { cache.first { it.first == key } }
        return state.second.map {
            it as? T
        }
    }

    override suspend fun toMap(): Map<String, Any?> =
        cache.map { it.first to it.second.value }.toMap()
}