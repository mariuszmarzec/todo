package com.marzec.todo.screen.taskdetails.model

import com.marzec.todo.model.Task
import com.marzec.todo.screen.tasks.model.RemoveDialog

sealed class TaskDetailsState(
    open val task: Task?
) {
    data class Data(
        override val task: Task,
        val removeTaskDialog: RemoveDialog?
    ) : TaskDetailsState(task)

    data class Loading(
        override val task: Task? = null
    ) : TaskDetailsState(task)

    data class Error(
        override val task: Task? = null,
        val message: String
    ) : TaskDetailsState(task)

    companion object {
        val INITIAL = Loading()
    }
}