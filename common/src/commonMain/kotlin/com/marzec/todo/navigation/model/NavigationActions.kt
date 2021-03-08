package com.marzec.todo.navigation.model

import androidx.compose.runtime.Composable

sealed class NavigationActions {
    data class Next(val screenProvider: @Composable () -> Unit) : NavigationActions()
    object Back : NavigationActions()
}