package com.marzec.todo.navigation.model

data class NavigationAction(
    val destination: Destination,
    val options: NavigationOptions? = null
)

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
        val taskToEditId: Int?,
        val parentTaskId: Int?
    ) : Destination()

    data class TaskDetails(val listId: Int, val taskId: Int) : Destination()
    data class AddSubTask(val listId: Int, val taskId: Int) : Destination()
}