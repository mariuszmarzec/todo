package com.marzec.todo.navigation.model

sealed class NavigationActions {
    data class Next(
        val destination: Destination,
        val options: NavigationOptions? = null
        ) : NavigationActions()
    object Back : NavigationActions()
}

data class NavigationOptions(
    val popTo: Destination,
    val popToInclusive: Boolean
)

sealed class Destination {

    object Login : Destination()
    object Lists : Destination()
    data class Tasks(val listId: Int) : Destination()
    data class AddNewTask(
        val listId: Int,
        val taskId: Int?,
        val parentTaskId: Int?
    ) : Destination()
    data class TaskDetails(val listId: Int, val taskId: Int) : Destination()
}