package com.marzec.preferences

import com.marzec.navigation.NavigationStateCache

class NavigationStateCacheProxy(private val stateCache: StateCache) : NavigationStateCache {

    override fun <T> get(key: String): T? = stateCache.get(key)

    override fun set(key: String, value: Any) {
        stateCache.set(key, value)
    }

    override fun remove(key: String) = stateCache.remove(key)
}
