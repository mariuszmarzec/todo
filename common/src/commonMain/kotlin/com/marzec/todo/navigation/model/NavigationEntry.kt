package com.marzec.todo.navigation.model

import androidx.compose.runtime.Composable

data class NavigationEntry(
    val destination: Destination,
    val cacheKey: String,
    val screenProvider: @Composable() (destination: Destination, cacheKey: String) -> Unit
)
