package com.marzec.todo.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.marzec.todo.navigation.model.NavigationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import androidx.compose.material.TopAppBar
import androidx.compose.ui.graphics.Color
import com.marzec.todo.navigation.model.NavigationActions
import com.marzec.todo.navigation.model.NavigationStore
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.TextButton
import kotlinx.coroutines.launch
import androidx.compose.material.Text

@Composable
fun MainScreen(navigationStore: NavigationStore) {
    val scope = rememberCoroutineScope()

    val state: NavigationState by navigationStore.state.collectAsState()

    state.backStack.last().apply {
        screenProvider(destination, cacheKey)
    }
}