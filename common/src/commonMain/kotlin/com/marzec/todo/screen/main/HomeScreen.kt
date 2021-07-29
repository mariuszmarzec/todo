package com.marzec.todo.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.todo.navigation.model.NavigationState
import com.marzec.todo.navigation.model.NavigationStore

@Composable
fun HomeScreen(navigationStore: NavigationStore) {
    val scope = rememberCoroutineScope()

    val state: NavigationState by navigationStore.state.collectAsState()

    LaunchedEffect(key1 = Unit) {
        navigationStore.init(scope)
    }

    state.backStack.last().apply {
        screenProvider(destination, cacheKey)
    }
}