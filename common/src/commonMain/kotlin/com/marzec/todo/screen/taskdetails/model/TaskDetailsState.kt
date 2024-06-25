package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task
import com.marzec.delegate.WithSearch
import com.marzec.delegate.WithSelection
import com.marzec.todo.delegates.dialog.WithTasks
import com.marzec.delegate.DialogState
import com.marzec.delegate.ScrollListState
import com.marzec.delegate.WithScrollListState
import com.marzec.todo.delegates.reorder.ReorderMode
import com.marzec.todo.delegates.reorder.WithReorderMode
import com.marzec.view.SearchState

data class TaskDetailsState(
    val task: Task,
    override val dialog: DialogState<Int>,
    override val selected: Set<Int>,
    override val search: SearchState,
    override val scrollListState: ScrollListState,
    override val reorderMode: ReorderMode
) : WithTasks<TaskDetailsState>,
    WithSelection<Int, TaskDetailsState>,
    WithScrollListState<TaskDetailsState>,
    WithSearch<TaskDetailsState>,
    WithReorderMode {

    override fun copyWithDialog(dialog: DialogState<Int>): TaskDetailsState = copy(dialog = dialog)

    override fun taskById(taskId: Int): Task = if (taskId == task.id) {
        task
    } else {
        task.subTasks.first { it.id == taskId }
    }

    override fun copyWithSelection(selected: Set<Int>): TaskDetailsState = copy(selected = selected)

    override fun copyWithSearch(search: SearchState): TaskDetailsState = copy(search = search)

    override fun allIds(): Set<Int> = task.subTasks.map { it.id }.toSet()

    override fun copyWithScrollListState(scrollListState: ScrollListState): TaskDetailsState =
        copy(scrollListState = scrollListState)

    companion object {
        val INITIAL = State.Loading<TaskDetailsState>()
    }
}
