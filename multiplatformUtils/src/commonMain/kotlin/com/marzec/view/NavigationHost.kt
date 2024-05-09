package com.marzec.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.marzec.cache.MemoryCache
import com.marzec.mvi.collectState
import com.marzec.navigation.Destination
import com.marzec.navigation.NavigationEntry
import com.marzec.navigation.NavigationState
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.ResultCache
import com.marzec.navigation.currentFlow
import com.marzec.navigation.currentScreen
import com.marzec.navigation.navigationState
import com.marzec.preferences.StateCache
import kotlinx.coroutines.CoroutineScope

@Composable
fun NavigationHost(
    navigationStore: NavigationStore,
    router: (Destination) -> @Composable (destination: Destination, cacheKey: String) -> Unit
) {
    val state: NavigationState by navigationStore.collectState()

    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    ) {
        state.backStack.currentScreen()?.apply {
            val screenProvider = router(destination)
            screenProvider(destination, cacheKey)
        }
    }
}

@Composable
fun NavigationHost(
    navigationStore: NavigationStore,
    router: (Destination, String) -> @Composable (destination: Destination, cacheKey: String) -> Unit
) {
    val state: NavigationState by navigationStore.collectState()

    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight()
    ) {
        val currentFlow = state.currentFlow()
        currentFlow.currentScreen()?.apply {
            val screenProvider = router(destination, currentFlow.id)
            screenProvider(destination, cacheKey)
        }
    }
}

fun navigationStore(
    scope: CoroutineScope,
    stateCache: StateCache,
    cacheKeyProvider: () -> String,
    navigationStoreCacheKey: String,
    defaultDestination: Destination,
    overrideLastClose: (NavigationState.() -> NavigationState)? = null,
    onNewStateCallback: ((NavigationState) -> Unit)? = null
): NavigationStore = NavigationStore(
    scope = scope,
    stateCache = stateCache,
    resultCache = ResultCache(MemoryCache()),
    cacheKey = navigationStoreCacheKey,
    cacheKeyProvider = cacheKeyProvider,
    initialState = navigationState(
        backStack = listOf(
            NavigationEntry(
                destination = defaultDestination,
                cacheKey = cacheKeyProvider()
            )
        )
    ),
    overrideLastClose = overrideLastClose,
    onNewStateCallback = onNewStateCallback
)
