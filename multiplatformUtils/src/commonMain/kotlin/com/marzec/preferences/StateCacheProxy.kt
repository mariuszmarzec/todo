package com.marzec.preferences

import com.marzec.navigation.NavigationStateCache
import kotlinx.coroutines.flow.Flow

class StateCacheProxy(private val stateCache: StateCache) : NavigationStateCache {

    override suspend fun <T> read(key: String): T? = stateCache.get(key)

    override suspend fun <T> write(key: String, value: T?) {
        if (value != null) {
            stateCache.set(key, value)
        } else {
            stateCache.remove(key)
        }
    }

    override suspend fun remove(key: String) = stateCache.remove(key)

    override suspend fun set(key: String, value: Any) = stateCache.set(key, value)

    override suspend fun <T> get(key: String): T? = stateCache.get(key)

    override suspend fun <T> observe(key: String): Flow<T?> = kotlinx.coroutines.flow.flowOf(get(key))
}
