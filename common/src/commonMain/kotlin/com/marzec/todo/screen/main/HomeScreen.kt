package com.marzec.todo.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.marzec.navigation.NavigationStore
import com.marzec.todo.DI
import com.marzec.view.LocalScrollListStateMap
import com.marzec.view.LocalScrollStateMap
import com.marzec.view.NavigationHost

@Composable
fun HomeScreen(navigationStore: NavigationStore) {

    LaunchedEffect(Unit) {
        DI.todoRepository.receiveSse()
    }

    CompositionLocalProvider(
        LocalScrollStateMap provides DI.scrollStateCache,
        LocalScrollListStateMap provides DI.listScrollStateCache
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().background(Color.White)
        ) {
            NavigationHost(navigationStore, DI::router)
        }
    }
}
