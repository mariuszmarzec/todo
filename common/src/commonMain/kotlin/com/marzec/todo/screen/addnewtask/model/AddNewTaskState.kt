package com.marzec.todo.screen.addnewtask.model

sealed class AddNewTaskState {

    data class Data(
        val data: TaskData
    ) : AddNewTaskState()

    object Loading : AddNewTaskState()

    data class Error(val message: String) : AddNewTaskState()

    companion object {
        fun initial(listId: Int, taskId: Int?) = Data(
            data = TaskData(
                taskId = taskId,
                listId = listId,
                description = ""
            )
        )
    }
}

data class TaskData(
    val taskId: Int?,
    val listId: Int,
    val description: String
)