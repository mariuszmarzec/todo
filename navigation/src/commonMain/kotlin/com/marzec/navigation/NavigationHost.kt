package com.marzec.navigation

import com.marzec.mvi.collectState
import kotlinx.coroutines.CoroutineScope

fun navigationStore(
    scope: CoroutineScope,
    stateCache: NavigationStateCache,
    resultCache: NavigationCache,
    cacheKeyProvider: () -> String,
    navigationStoreCacheKey: String,
    defaultDestination: Destination,
    overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null,
    onNewStateCallback: ((NavigationState) -> Unit)? = null,
    onAfterClosed: ((entry: NavigationEntry) -> Unit)? = null
): NavigationStore = navigationStore(
    scope,
    stateCache,
    resultCache,
    navigationStoreCacheKey,
    cacheKeyProvider,
    initialState(defaultDestination, cacheKeyProvider),
    overrideLastClose,
    onNewStateCallback,
    onAfterClosed
)

fun navigationStore(
    scope: CoroutineScope,
    stateCache: NavigationStateCache,
    resultCache: NavigationCache,
    navigationStoreCacheKey: String,
    cacheKeyProvider: () -> String,
    initialState: NavigationState,
    overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null,
    onNewStateCallback: ((NavigationState) -> Unit)? = null,
    onAfterClosed: ((entry: NavigationEntry) -> Unit)? = null
) = NavigationStore(
    scope = scope,
    stateCache = stateCache,
    resultCache = ResultCache(resultCache),
    cacheKey = navigationStoreCacheKey,
    cacheKeyProvider = cacheKeyProvider,
    initialState = initialState,
    overrideLastClose = overrideLastClose,
    onAfterClosed = onAfterClosed
).apply {
    if (onNewStateCallback != null) {
        this.onNewStateCallback = onNewStateCallback
    }
}

fun initialState(
    defaultDestination: Destination,
    cacheKeyProvider: () -> String
) = navigationState(
    backStack = listOf(
        NavigationEntry(
            destination = defaultDestination,
            cacheKey = cacheKeyProvider()
        )
    )
)
