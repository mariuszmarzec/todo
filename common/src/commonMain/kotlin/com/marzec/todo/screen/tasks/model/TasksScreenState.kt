package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.State
import com.marzec.todo.delegates.dialog.WithDialog
import com.marzec.todo.extensions.EMPTY_STRING
import com.marzec.todo.model.Task
import com.marzec.todo.view.DialogState

data class TasksScreenState(
    val tasks: List<Task>,
    val search: String,
    val searchFocused: Boolean,
    override val dialog: DialogState
) : WithDialog<TasksScreenState> {

    override fun copyWithDialog(dialog: DialogState): TasksScreenState = copy(dialog = dialog)

    companion object {
        val INITIAL_STATE = State.Loading<TasksScreenState>()
        val EMPTY_DATA = TasksScreenState(
            tasks = emptyList(),
            search = EMPTY_STRING,
            searchFocused = false,
            dialog = DialogState.NoDialog
        )
    }
}
