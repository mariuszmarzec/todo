package com.marzec.navigation

import kotlinx.coroutines.CoroutineScope

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

fun navigationStore(
    scope: CoroutineScope,
    stateCache: NavigationStateCache,
    cacheKeyProvider: () -> String,
    navigationStoreCacheKey: String,
    defaultDestination: Destination,
    overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null,
    onNewStateCallback: ((NavigationState) -> Unit)? = null,
    onAfterClosed: ((entry: NavigationEntry) -> Unit)? = null
): NavigationStore = navigationStore(
    scope,
    stateCache,
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
    navigationStoreCacheKey: String,
    cacheKeyProvider: () -> String,
    initialState: NavigationFlow,
    overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null,
    onNewStateCallback: ((NavigationState) -> Unit)? = null,
    onAfterClosed: ((entry: NavigationEntry) -> Unit)? = null
) = NavigationStore(
    scope = scope,
    navigationStateCache = stateCache,
    resultCache = ResultCache(NavigationCacheProxy(MemoryCache())),
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
