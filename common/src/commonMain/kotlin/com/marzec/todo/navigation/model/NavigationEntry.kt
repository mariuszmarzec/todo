package com.marzec.todo.navigation.model

import androidx.compose.runtime.Composable

data class NavigationEntry(
    val cacheKey: String,
    val screenProvider: @Composable (cacheKey: String) -> Unit
)