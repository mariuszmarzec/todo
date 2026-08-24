package com.marzec.navigation

import kotlinx.coroutines.flow.Flow

/**
 * Navigation-owned interface for state caching.
 * This wraps [com.marzec.preferences.StateCache] so navigation doesn't depend on the preferences module.
 */
interface NavigationStateCache {

    suspend fun set(key: String, value: Any)

    suspend fun <T> get(key: String): T?

    suspend fun remove(key: String)

    suspend fun <T> observe(key: String): Flow<T?>

    suspend fun toMap(): Map<String, Any?>
}