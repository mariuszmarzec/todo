package com.marzec.navigation

import kotlinx.coroutines.flow.Flow

/**
 * Navigation-owned interface for state persistence.
 * Combines reading and writing capabilities needed by NavigationStore.
 * Clients implement this interface to provide their own state storage.
 */
interface NavigationStateCache {
    suspend fun <T> read(key: String): T?
    suspend fun write(key: String, value: Any?)
    suspend fun remove(key: String)
}