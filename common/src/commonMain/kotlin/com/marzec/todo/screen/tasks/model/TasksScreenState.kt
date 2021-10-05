package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.State
import com.marzec.todo.extensions.emptyString
import com.marzec.todo.model.Task
import com.marzec.todo.view.DialogState

data class TasksScreenState(
    val tasks: List<Task>,
    val search: String,
    val searchFocused: Boolean,
    val dialog: DialogState
) {
    companion object {
        val INITIAL_STATE = State.Loading<TasksScreenState>()
        val EMPTY_DATA = TasksScreenState(
            tasks = emptyList(),
            search = emptyString(),
            searchFocused = false,
            dialog = DialogState.NoDialog
        )
    }
}
