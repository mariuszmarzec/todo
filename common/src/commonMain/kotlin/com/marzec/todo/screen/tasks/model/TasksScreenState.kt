package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task
import com.marzec.todo.view.DialogState

data class TasksScreenState(
    val tasks: List<Task>,
    val removeTaskDialog: DialogState
) {
    companion object {
        val INITIAL_STATE = State.Loading<TasksScreenState>()
        val EMPTY_DATA = TasksScreenState(
            tasks = emptyList(),
            removeTaskDialog = DialogState.NoDialog
        )
    }
}
