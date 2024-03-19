package com.marzec.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

class CompositeCacheSaver<T : Any>(private val savers: List<CacheSaver<T>>) : CacheSaver<T> {

    override suspend fun get(): T? = savers.firstOrNull()?.get()

    override suspend fun observeCached(): Flow<T?> =
        savers.mapIndexed { index, saver ->
            saver.observeCached().filterNotNull().let { flow ->
                if (index > 0) {
                    flow.onEach { newValue ->
                        savers.firstOrNull()?.updateCache(newValue)
                    }
                } else {
                    flow
                }
            }
        }.merge()
            .distinctUntilChanged()

    override suspend fun updateCache(data: T) {
        savers.forEach {
            it.updateCache(data)
        }
    }

    override suspend fun updateCache(update: (T?) -> T?) {
        savers.forEach {
            it.updateCache(update)
        }
    }
}