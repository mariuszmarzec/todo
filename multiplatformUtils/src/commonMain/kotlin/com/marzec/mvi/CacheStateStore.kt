package com.marzec.mvi

import com.marzec.preferences.StateCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
fun <State : Any> CacheStateStore(
    scope: CoroutineScope,
    defaultState: State,
    stateCache: StateCache,
    cacheKey: String,
    onNewStateCallback: (State) -> Unit = { }
): Store4<State> = CacheStateStore(
    store = Store4Impl(
        scope = scope,
        defaultState = stateCache.get<State>(cacheKey) ?: defaultState,
        onNewStateCallback = {
            stateCache.set(cacheKey, it)
            onNewStateCallback(it)
        }
    ),
    cacheKey = cacheKey
)

private class CacheStateStore<State : Any>(
    private val store: Store4<State>,
    cacheKey: String,
) : Store4<State> by store {

    override val identifier = cacheKey
}
