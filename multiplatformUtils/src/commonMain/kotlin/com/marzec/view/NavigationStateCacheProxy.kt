package com.marzec.view

import com.marzec.navigation.NavigationCache
import com.marzec.navigation.NavigationStateCache

/**
 * Adapts [NavigationCache] to the navigation-owned [NavigationStateCache] contract.
 *
 * Used by client-side wiring in DI. Clients may provide their own implementation instead.
 */
class NavigationStateCacheProxy(private val cache: NavigationCache) : NavigationStateCache {

    override fun <T> get(key: String): T? = cache.get(key)

    override fun set(key: String, value: Any) {
        cache.put(key, value)
    }

    override fun remove(key: String) = cache.remove(key)
}
