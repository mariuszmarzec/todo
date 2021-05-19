package com.marzec.todo.screen.addnewtask.model

sealed class AddNewTaskState {

    data class Data(
        val data: TaskData
    ) : AddNewTaskState()

    object Loading : AddNewTaskState()

    data class Error(val message: String) : AddNewTaskState()

    companion object {
        fun initial(listId: Int, taskId: Int?, parentTaskId: Int?) = Data(
            data = TaskData(
                taskId = taskId,
                parentTaskId = parentTaskId,
                listId = listId,
                description = "",
                priority = 0,
                isToDo = true
            )
        )
    }
}

data class TaskData(
    val taskId: Int?,
    val parentTaskId: Int?,
    val listId: Int,
    val priority: Int,
    val isToDo: Boolean,
    val description: String
)