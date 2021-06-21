package com.marzec.todo.screen.addnewtask.model

data class AddNewTaskState(
    val taskId: Int?,
    val parentTaskId: Int?,
    val listId: Int,
    val priority: Int,
    val isToDo: Boolean,
    val description: String
) {

    companion object {
        fun initial(listId: Int, taskId: Int?, parentTaskId: Int?) = AddNewTaskState(
            taskId = taskId,
            parentTaskId = parentTaskId,
            listId = listId,
            description = "",
            priority = 0,
            isToDo = true
        )
    }
}
