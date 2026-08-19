package com.marzec.navigation

/**
 * Navigation-owned interface for state persistence.
 * Combines reading and writing capabilities needed by NavigationStore.
 * Clients implement this interface (via StateCache or a custom proxy) to provide
 * their own state storage.
 */
interface NavigationStateCache : StateReader, StateEditor {

    /**
     * Sets a value in the cache.
     * Used for writing navigation state.
     */
    fun set(key: String, value: Any)

    /**
     * Gets a value from the cache.
     * Used for reading navigation state.
     */
    fun <T> get(key: String): T?

    /**
     * Removes a value from the cache.
     * Used when navigation entries are popped.
     */
    fun remove(key: String)
}