package com.marzec.todo.screen.taskdetails.model

import com.marzec.todo.model.Task

sealed class TaskDetailsState {
    data class Data(
        val task: Task
    ) : TaskDetailsState()
    object Loading : TaskDetailsState()
    data class Error(val message: String) : TaskDetailsState()

    companion object {
        val INITIAL = Loading
    }
}