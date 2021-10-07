package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task
import com.marzec.todo.view.DialogState

data class TaskDetailsState(
    val task: Task,
    val dialog: DialogState
) {
    companion object {
        val INITIAL = State.Loading<TaskDetailsState>()
    }
}