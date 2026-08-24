package com.marzec.view

import com.marzec.navigation.NavigationStateCache
import com.marzec.preferences.StateCache
import kotlinx.coroutines.flow.Flow

/**
 * Adapts [StateCache] to the navigation-owned [NavigationStateCache] contract.
 *
 * Used by library-internal wiring ([NavigationHost] factories). Clients may
 * provide their own proxy instead.
 */
class NavigationStateCacheProxy(private val stateCache: StateCache) : NavigationStateCache {

    override suspend fun set(key: String, value: Any) = stateCache.set(key, value)

    override suspend fun <T> get(key: String): T? = stateCache.get(key)

    override suspend fun remove(key: String) = stateCache.remove(key)

    override suspend fun <T> observe(key: String): Flow<T?> = stateCache.observe(key)

    override suspend fun toMap(): Map<String, Any?> = stateCache.toMap()
}