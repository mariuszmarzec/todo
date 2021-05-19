package com.marzec.todo.screen.taskdetails.model

import com.marzec.todo.model.Task
import com.marzec.todo.screen.tasks.model.RemoveDialog

sealed class TaskDetailsState {
    data class Data(
        val task: Task,
        val removeTaskDialog: RemoveDialog
    ) : TaskDetailsState()
    object Loading : TaskDetailsState()
    data class Error(val message: String) : TaskDetailsState()

    companion object {
        val INITIAL = Loading
    }
}