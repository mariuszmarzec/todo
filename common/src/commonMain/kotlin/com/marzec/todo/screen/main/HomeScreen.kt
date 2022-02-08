package com.marzec.todo.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.marzec.todo.extensions.collectState
import com.marzec.todo.navigation.NavigationState
import com.marzec.todo.navigation.NavigationStore

@Composable
fun HomeScreen(navigationStore: NavigationStore) {
    val state: NavigationState by navigationStore.collectState()

    state.backStack.last().apply {
        screenProvider(destination, cacheKey)
    }
}
