package com.marzec.navigation

/**
 * Navigation-owned interface for state persistence.
 * Combines reading and writing capabilities needed by NavigationStore.
 * Clients implement this interface to provide their own state storage.
 */
interface NavigationStateCache {
    fun <T> get(key: String): T?
    fun set(key: String, value: Any)
    fun remove(key: String)
}
