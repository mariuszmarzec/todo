package com.marzec.navigation

import kotlinx.coroutines.flow.Flow

/**
 * Adapts [NavigationCache] to the navigation-owned [NavigationStateCache] contract.
 *
 * Used by library-internal wiring ([NavigationHost] factories). Clients may
 * provide their own implementation instead.
 */
class NavigationStateCacheProxy(private val cache: NavigationCache) : NavigationStateCache {

    override suspend fun put(key: String, value: Any?) = cache.put(key, value)

    override suspend fun <T> get(key: String): T? = cache.get(key)

    override suspend fun remove(key: String) = cache.remove(key)

    override suspend fun <T> observe(key: String): Flow<T?> = cache.observe(key)

    override suspend fun toMap(): Map<String, Any?> = cache.toMap()

    override fun set(key: String, value: Any) = put(key, value)

    override suspend fun read(key: String): Any? = get(key)

    override suspend fun write(key: String, value: Any?) {
        if (value != null) {
            put(key, value)
        } else {
            remove(key)
        }
    }
}