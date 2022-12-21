package com.marzec.todo.screen.tasks.model

import com.marzec.delegate.DialogState
import com.marzec.delegate.WithSearch
import com.marzec.extensions.EMPTY_STRING
import com.marzec.mvi.State
import com.marzec.todo.delegates.dialog.WithTasks
import com.marzec.todo.model.Task
import com.marzec.view.SearchState

data class TasksScreenState(
    val tasks: List<Task>,
    val scrollState: ScrollListState,
    override val search: SearchState,
    override val dialog: DialogState
) : WithTasks<TasksScreenState>, WithSearch<TasksScreenState> {

    override fun copyWithDialog(dialog: DialogState): TasksScreenState = copy(dialog = dialog)

    override fun copyWithSearch(search: SearchState): TasksScreenState =
        copy(search = search)

    override fun taskById(taskId: Int): Task = tasks.first { it.id == taskId }

    companion object {
        val INITIAL_STATE = State.Loading<TasksScreenState>()
        val EMPTY_DATA = TasksScreenState(
            tasks = emptyList(),
            scrollState = ScrollListState(),
            search = SearchState(
                value = EMPTY_STRING,
                focused = false,
            ),
            dialog = DialogState.NoDialog
        )
    }
}

data class ScrollListState(
    val index: Int = 0,
    val offset: Int = 0
)