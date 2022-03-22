package com.marzec.todo.screen.addnewtask.model

import com.marzec.todo.model.Scheduler

data class AddNewTaskState(
    val taskId: Int?,
    val parentTaskId: Int?,
    val priority: Int,
    val isToDo: Boolean,
    val description: String,
    val highestPriorityAsDefault: Boolean,
    val scheduler: Scheduler? = null,
    val fetched: Boolean
) {

    companion object {
        fun initial(taskId: Int?, parentTaskId: Int?) = AddNewTaskState(
            taskId = taskId,
            parentTaskId = parentTaskId,
            description = "",
            priority = 0,
            isToDo = true,
            highestPriorityAsDefault = false,
            fetched = false
        )
    }
}
