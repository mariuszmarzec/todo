package com.marzec.view

import com.marzec.navigation.NavigationStateCache
import com.marzec.preferences.StateCache

/**
 * Adapts [StateCache] to the navigation-owned [NavigationStateCache] contract.
 *
 * Used by client-side wiring in DI. Clients may provide their own implementation instead.
 */
class NavigationStateCacheProxy(private val stateCache: StateCache) : NavigationStateCache {

    override suspend fun <T> read(key: String): T? = stateCache.get(key)

    override suspend fun write(key: String, value: Any?) {
        if (value != null) {
            stateCache.set(key, value)
        } else {
            stateCache.remove(key)
        }
    }

    override suspend fun remove(key: String) = stateCache.remove(key)

    override suspend fun <T> get(key: String): T? = stateCache.get(key)

    override suspend fun <T> observe(key: String) = kotlinx.coroutines.flow.flowOf(get(key))
}
