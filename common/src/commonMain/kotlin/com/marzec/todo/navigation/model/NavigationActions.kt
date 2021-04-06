package com.marzec.todo.navigation.model

sealed class NavigationActions {
    data class Next(val destination: Destination) : NavigationActions()
    object Back : NavigationActions()
}

sealed class Destination {
    object Login : Destination()
    object Lists : Destination()
    data class Tasks(val listId: Int) : Destination()
}