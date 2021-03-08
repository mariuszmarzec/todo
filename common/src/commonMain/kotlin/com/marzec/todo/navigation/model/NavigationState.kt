package com.marzec.todo.navigation.model

import androidx.compose.runtime.Composable

data class NavigationState(
    val stack: List<@Composable () -> Unit>
)