package com.marzec.todo.navigation.model

sealed class NavigationActions {
    data class Next(val destination: Destinations) : NavigationActions()
    object Back : NavigationActions()
}

enum class Destinations {
    LOGIN,
    LISTS
}