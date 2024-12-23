package com.marzec.navigation

import com.marzec.cache.Cache

class NavigationEntryCache(
    private val navigationStore: NavigationStore,
    private val cache: Cache
) : Cache by cache {

    override suspend fun put(key: String, value: Any?) {
        if (navigationStore.state.value.contains(key)) {
            cache.put(key, value)
        }
    }

    private fun NavigationState.contains(cacheKey: String) =
        backStack.any { it.contains(cacheKey) }

    private fun NavigationEntry.contains(cacheKey: String): Boolean =
        cacheKey == this.cacheKey || subFlow?.contains(cacheKey) == true
}