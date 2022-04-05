package com.marzec.todo.screen.addnewtask.model

import com.marzec.mvi.State
import com.marzec.todo.model.Scheduler

data class AddNewTaskState(
    val taskId: Int?,
    val parentTaskId: Int?,
    val priority: Int,
    val isToDo: Boolean,
    val description: String,
    val highestPriorityAsDefault: Boolean,
    val scheduler: Scheduler? = null
) {

    companion object {

        fun default(
            taskId: Int? = null,
            parentTaskId: Int? = null
        ) = AddNewTaskState(
            taskId = taskId,
            parentTaskId = parentTaskId,
            description = "",
            priority = 0,
            isToDo = true,
            highestPriorityAsDefault = false,
        )

        fun initial(taskId: Int?, parentTaskId: Int?): State<AddNewTaskState> = taskId?.let {
            State.Loading(
                default(
                    taskId = taskId,
                    parentTaskId = parentTaskId
                )
            )
        } ?: State.Data(default(parentTaskId = parentTaskId))
    }
}
