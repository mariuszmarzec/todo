package com.marzec.navigation

import kotlinx.coroutines.flow.Flow

/**
 * Adapts [NavigationCache] to the navigation-owned [NavigationStateCache] contract.
 *
 * Used by library-internal wiring ([NavigationHost] factories). Clients may
 * provide their own implementation instead.
 */
class NavigationStateCacheProxy(private val cache: NavigationCache) : NavigationStateCache {

    override suspend fun <T> read(key: String): T? = cache.get(key)

    override suspend fun write(key: String, value: Any?) {
        if (value != null) {
            cache.put(key, value)
        } else {
            cache.remove(key)
        }
    }

    override suspend fun remove(key: String) = cache.remove(key)
}