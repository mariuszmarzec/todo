package com.marzec.todo.navigation

sealed class TodoDestination: com.marzec.navigation.Destination {

    object Login : TodoDestination()
    object Tasks : TodoDestination()
    data class AddNewTask(
        val taskToEditId: Int?,
        val parentTaskId: Int?
    ) : TodoDestination()

    data class TaskDetails(val taskId: Int) : TodoDestination()
    data class AddSubTask(val taskId: Int) : TodoDestination()
}
