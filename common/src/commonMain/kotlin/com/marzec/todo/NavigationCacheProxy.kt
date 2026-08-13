package com.marzec.todo

import com.marzec.navigation.NavigationCache
import com.marzec.preferences.StateCache

class NavigationCacheProxy(private val stateCache: StateCache) : NavigationCache {
    override fun set(key: String, value: Any) {
        stateCache.set(key, value)
    }

    override fun <T> get(key: String): T? {
        return stateCache.get(key)
    }

    override fun remove(key: String) {
        stateCache.remove(key)
    }
}