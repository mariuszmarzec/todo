package com.marzec.todo.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marzec.todo.DI
import com.marzec.view.NavigationHost

@Composable
fun HomeScreenSaveable() {

    val viewModel: NavigationViewModel = viewModel()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(Color.White)
    ) {
        NavigationHost(viewModel.store, DI::router)
    }
}

class NavigationViewModel(private val state: SavedStateHandle) : ViewModel() {

    val store = DI.provideNavigationStore(
        scope = viewModelScope,
        state["state"] ?: DI.navigationInitialState()
    ) { newState ->
        state["state"] = newState
    }.also {
        DI.navigationStore = it
    }
}