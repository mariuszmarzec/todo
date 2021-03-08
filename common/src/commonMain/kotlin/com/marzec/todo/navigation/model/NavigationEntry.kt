package com.marzec.todo.navigation.model

import androidx.compose.runtime.Composable

data class NavigationEntry(val screenProvider: @Composable () -> Unit)