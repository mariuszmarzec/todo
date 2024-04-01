package com.marzec.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

class CompositeUpdatableCacheSaver<T : Any>(
    private val savers: List<UpdatableCacheSaver<T>>
) : UpdatableCacheSaver<T> {

    override suspend fun get(): T? = savers.firstOrNull()?.get()

    override suspend fun observeCached(): Flow<T?> =
        savers.mapIndexed { index, saver ->
            saver.observeCached().filterNotNull().let { flow ->
                if (index > 0) {
                    flow.distinctUntilChanged()
                        .onEach { newValue ->
                            savers.firstOrNull()?.saveCache(newValue)
                        }
                } else {
                    flow
                }
            }
        }.merge()
            .distinctUntilChanged()

    override suspend fun saveCache(data: T) {
        savers.forEach {
            it.saveCache(data)
        }
    }

    override suspend fun updateCache(update: (T?) -> T?) {
        savers.forEach {
            it.updateCache(update)
        }
    }
}
