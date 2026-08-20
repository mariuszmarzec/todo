package com.marzec.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.marzec.navigation.Destination
import com.marzec.navigation.NavigationEntry
import com.marzec.navigation.NavigationFlow
import com.marzec.navigation.NavigationState
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.currentFlow
import com.marzec.navigation.currentScreen

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
