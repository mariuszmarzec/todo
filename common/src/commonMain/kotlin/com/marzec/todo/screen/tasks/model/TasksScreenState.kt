package com.marzec.todo.screen.tasks.model

import com.marzec.delegate.DialogState
import com.marzec.delegate.WithSearch
import com.marzec.delegate.WithSelection
import com.marzec.extensions.EMPTY_STRING
import com.marzec.mvi.State
import com.marzec.todo.delegates.dialog.WithTasks
import com.marzec.todo.delegates.reorder.ReorderMode
import com.marzec.todo.delegates.reorder.WithReorderMode
import com.marzec.todo.model.Task
import com.marzec.view.SearchState

data class TasksScreenState(
    val tasks: List<Task>,
    override val search: SearchState,
    override val dialog: DialogState<Int>,
    override val selected: Set<Int>,
    override val reorderMode: ReorderMode,
    val doneButtonOnTaskList: Boolean
) : WithTasks<TasksScreenState>,
    WithSearch<TasksScreenState>,
    WithSelection<Int, TasksScreenState>,
    WithReorderMode {

    override fun copyWithDialog(dialog: DialogState<Int>): TasksScreenState = copy(dialog = dialog)

    override fun copyWithSearch(search: SearchState): TasksScreenState =
        copy(search = search)

    override fun copyWithSelection(selected: Set<Int>): TasksScreenState =
        copy(selected = selected)

    override fun allIds(): Set<Int> = tasks.map { it.id }.toSet()

    override fun taskById(taskId: Int): Task = tasks.first { it.id == taskId }

    companion object {

        fun initial(): State<TasksScreenState> =
            State.Loading(emptyData())

        fun emptyData() = TasksScreenState(
            tasks = emptyList(),
            search = SearchState(
                value = EMPTY_STRING,
                focused = false,
            ),
            dialog = DialogState.NoDialog(),
            selected = emptySet(),
            reorderMode = ReorderMode.Disabled,
            doneButtonOnTaskList = false
        )
    }
}
