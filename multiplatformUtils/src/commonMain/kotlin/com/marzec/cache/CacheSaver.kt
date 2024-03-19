package com.marzec.cache

import kotlinx.coroutines.flow.Flow

interface CacheSaver<T> {

    suspend fun get(): T?

    suspend fun observeCached(): Flow<T?>

    suspend fun updateCache(data: T)

    suspend fun updateCache(update: (T?) -> T?)
}