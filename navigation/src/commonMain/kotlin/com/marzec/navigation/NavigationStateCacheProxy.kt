package com.marzec.navigation

import kotlinx.coroutines.flow.Flow

/**
 * Adapts [NavigationCache] to the navigation-owned [NavigationStateCache] contract.
 *
 * Used by library-internal wiring ([NavigationHost] factories). Clients may
 * provide their own implementation instead.
 */
class NavigationStateCacheProxy(private val cache: NavigationCache) : NavigationStateCache {

    override fun <T> get(key: String): T? = cache.get(key)

    override fun set(key: String, value: Any) {
        if (value != null) {
            cache.put(key, value)
        } else {
            cache.remove(key)
        }
    }

    override fun remove(key: String) = cache.remove(key)
}
