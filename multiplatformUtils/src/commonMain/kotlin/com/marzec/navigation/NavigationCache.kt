package com.marzec.navigation


import kotlinx.coroutines.flow.Flow

interface NavigationCache {

    suspend fun put(key: String, value: Any?)

    suspend fun <T> get(key: String): T?

    suspend fun remove(key: String)

    suspend fun <T> observe(key: String): Flow<T?>

    suspend fun toMap(): Map<String, Any?>
}

interface NavigationCache {
    fun set(key: String, value: Any)
    fun <T> get(key: String): T?
    fun remove(key: String)
}

