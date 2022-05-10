package com.marzec.todo.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.marzec.mvi.collectState
import com.marzec.navigation.NavigationState
import com.marzec.navigation.NavigationStore

@Composable
fun HomeScreen(navigationStore: NavigationStore) {
    val state: NavigationState by navigationStore.collectState()

    Box(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().background(Color.White)
    ) {
        state.backStack.last().apply {
            screenProvider(destination, cacheKey)
        }
    }
}
