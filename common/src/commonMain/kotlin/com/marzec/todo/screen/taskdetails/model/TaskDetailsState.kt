package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task
import com.marzec.delegate.WithSearch
import com.marzec.delegate.WithSelection
import com.marzec.todo.delegates.dialog.WithTasks
import com.marzec.delegate.DialogState
import com.marzec.view.SearchState

data class TaskDetailsState(
    val task: Task,
    override val dialog: DialogState,
    override val selected: Set<Int>,
    override val search: SearchState
) : WithTasks<TaskDetailsState>,
    WithSelection<Int, TaskDetailsState>,
    WithSearch<TaskDetailsState> {

    override fun copyWithDialog(dialog: DialogState): TaskDetailsState = copy(dialog = dialog)

    override fun taskById(taskId: Int): Task = if (taskId == task.id) {
        task
    } else {
        task.subTasks.first { it.id == taskId }
    }

    override fun copyWithSelection(selected: Set<Int>): TaskDetailsState = copy(selected = selected)

    override fun copyWithSearch(search: SearchState): TaskDetailsState = copy(search = search)

    override fun allIds(): Set<Int> = task.subTasks.map { it.id }.toSet()

    companion object {
        val INITIAL = State.Loading<TaskDetailsState>()
    }
}
