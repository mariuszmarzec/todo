package com.marzec.mvi

import com.marzec.preferences.StateCache
import kotlinx.coroutines.flow.MutableStateFlow

fun <State : Any> Store4<State>.toCachable(
    stateCache: StateCache,
    cacheKey: String
): Store4<State> = CacheStateStore(this, stateCache, cacheKey)

private class CacheStateStore<State : Any>(
    private val store: Store4<State>,
    stateCache: StateCache,
    cacheKey: String,
) : Store4<State> by store {

    init {
        val defaultStateInitializer = store.stateInitializer
        store.stateInitializer = {
            stateCache.get<State>(cacheKey)?.let {
                MutableStateFlow(it)
            } ?: defaultStateInitializer()
        }
        store.onNewStateCallback = {
            stateCache.set(cacheKey, it)
        }
    }

    override val identifier = cacheKey
}
