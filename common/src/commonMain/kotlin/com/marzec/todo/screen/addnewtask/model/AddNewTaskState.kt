package com.marzec.todo.screen.addnewtask.model

import com.marzec.mvi.State
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task

data class AddNewTaskState(
    val taskId: Int?,
    val task: Task?,
    val parentTaskId: Int?,
    val priority: Int,
    val isToDo: Boolean,
    val description: String,
    val highestPriorityAsDefault: Boolean,
    val removeAfterSchedule: Boolean,
    val showNotification: Boolean,
    val scheduler: Scheduler? = null
) {

    val schedulerWithOptions: Scheduler?
        get() = when (scheduler) {
            is Scheduler.Monthly -> scheduler.copy(
                highestPriorityAsDefault = highestPriorityAsDefault,
                showNotification = showNotification,
            )
            is Scheduler.OneShot -> scheduler.copy(
                highestPriorityAsDefault = highestPriorityAsDefault,
                removeScheduled = removeAfterSchedule,
                        showNotification = showNotification,
            )

            is Scheduler.Weekly -> scheduler.copy(
                highestPriorityAsDefault = highestPriorityAsDefault,
                showNotification = showNotification,
            )
            else -> null
        }

    companion object {

        fun default(
            taskId: Int? = null,
            parentTaskId: Int? = null,
        ) = AddNewTaskState(
            taskId = taskId,
            task = null,
            parentTaskId = parentTaskId,
            description = "",
            priority = 0,
            isToDo = true,
            highestPriorityAsDefault = false,
            removeAfterSchedule = false,
            showNotification = false,
        )

        fun initial(
            taskId: Int?,
            parentTaskId: Int?
        ): State<AddNewTaskState> = taskId?.let {
            State.Loading(
                default(
                    taskId = taskId,
                    parentTaskId = parentTaskId
                )
            )
        } ?: State.Data(
            default(
                parentTaskId = parentTaskId
            )
        )
    }
}
