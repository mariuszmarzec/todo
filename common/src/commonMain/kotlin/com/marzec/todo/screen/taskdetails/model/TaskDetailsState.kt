package com.marzec.todo.screen.taskdetails.model

import com.marzec.todo.model.Task
import com.marzec.todo.view.DialogState

sealed class TaskDetailsState(
    open val task: Task?
) {
    data class Data(
        override val task: Task,
        val dialog: DialogState
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