package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task
import com.marzec.todo.delegates.dialog.WithDialog
import com.marzec.todo.delegates.dialog.WithTasks
import com.marzec.todo.view.DialogState

data class TaskDetailsState(
    val task: Task,
    override val dialog: DialogState
): WithTasks<TaskDetailsState> {

    override fun copyWithDialog(dialog: DialogState): TaskDetailsState = copy(dialog = dialog)

    override fun taskById(taskId: Int): Task = if (taskId == task.id) {
        task
    } else {
        task.subTasks.first { it.id == taskId }
    }
    companion object {
        val INITIAL = State.Loading<TaskDetailsState>()
    }
}
