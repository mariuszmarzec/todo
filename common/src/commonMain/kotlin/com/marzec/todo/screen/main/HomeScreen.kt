package com.marzec.todo.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.todo.DI
import com.marzec.todo.extensions.collectState
import com.marzec.todo.navigation.model.NavigationState
import com.marzec.todo.navigation.model.NavigationStore

@Composable
fun HomeScreen(navigationStore: NavigationStore) {
    val state: NavigationState by navigationStore.collectState()

    LaunchedEffect(key1 = Unit) {
        navigationStore.init(DI.navigationScope)
    }

    state.backStack.last().apply {
        screenProvider(destination, cacheKey)
    }
}
