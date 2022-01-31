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
    object Tasks : Destination()
    data class AddNewTask(
        val taskToEditId: Int?,
        val parentTaskId: Int?
    ) : Destination()

    data class TaskDetails(val taskId: Int) : Destination()
    data class AddSubTask(val taskId: Int) : Destination()
}
