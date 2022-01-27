package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task
import com.marzec.todo.delegates.dialog.WithDialog
import com.marzec.todo.delegates.dialog.WithSelection
import com.marzec.todo.delegates.dialog.WithTasks
import com.marzec.todo.view.DialogState

data class TaskDetailsState(
    val task: Task,
    override val dialog: DialogState,
    override val selected: Set<Int>
) : WithTasks<TaskDetailsState>,
    WithSelection<TaskDetailsState> {

    override fun copyWithDialog(dialog: DialogState): TaskDetailsState = copy(dialog = dialog)

    override fun taskById(taskId: Int): Task = if (taskId == task.id) {
        task
    } else {
        task.subTasks.first { it.id == taskId }
    }

    override fun copyWithSelection(selected: Set<Int>): TaskDetailsState = copy(selected = selected)

    companion object {
        val INITIAL = State.Loading<TaskDetailsState>()
    }
}
