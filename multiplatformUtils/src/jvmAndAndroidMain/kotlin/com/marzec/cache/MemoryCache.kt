package com.marzec.cache

import java.util.Collections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MemoryCache(cacheSize: Int = MAX_CACHE_SIZE) : Cache {

    private val cache: MutableMap<String, MutableStateFlow<Any?>> =
        object : LinkedHashMap<String, MutableStateFlow<Any?>>() {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, MutableStateFlow<Any?>>?
            ): Boolean = size > cacheSize
        }.let { Collections.synchronizedMap(it) }

    override suspend fun put(key: String, value: Any?) {
        if (cache.containsKey(key)) {
            cache[key]?.emit(value)
        } else {
            cache[key] = MutableStateFlow(value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> get(key: String): T? {
        return cache[key]?.value as? T
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> observe(key: String): Flow<T?> {
        val state = cache[key] ?: put(
            key,
            null
        ).let { cache[key]!! }
        return state.map {
            it as? T
        }
    }

    override suspend fun toMap(): Map<String, Any?> = cache.mapValues { it.value.value }

    companion object {
        private const val MAX_CACHE_SIZE = 10
    }
}