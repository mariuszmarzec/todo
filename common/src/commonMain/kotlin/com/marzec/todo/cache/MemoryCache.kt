package com.marzec.todo.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MemoryCache {

    private val cache = ArrayList<Pair<String, MutableStateFlow<Any?>>>()

    suspend fun put(key: String, value: Any?) {
        if (cache.any { it.first == key }) {
            cache.first { it.first == key }.second.emit(value)
        } else {
            cache.add(0, key to MutableStateFlow(value))
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> observe(key: String): Flow<T?> {
        val state = cache.firstOrNull { it.first == key } ?:
        put(key, null).let { cache.first { it.first == key } }
        return state.second.map {
            it as? T
        }
    }
}