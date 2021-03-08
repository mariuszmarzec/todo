package com.marzec.todo.screen.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.marzec.todo.navigation.model.NavigationState
import com.marzec.todo.navigation.model.NavigationStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@Composable
fun MainScreen(navigationStore: NavigationStore) {

    val state: NavigationState by navigationStore.state.collectAsState()

    state.stack.last()()
}