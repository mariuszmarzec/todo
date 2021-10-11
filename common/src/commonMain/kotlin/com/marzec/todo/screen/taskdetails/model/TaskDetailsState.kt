package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task
import com.marzec.todo.delegates.dialog.WithDialog
import com.marzec.todo.view.DialogState

data class TaskDetailsState(
    val task: Task,
    override val dialog: DialogState
): WithDialog<TaskDetailsState> {

    override fun copyWithDialog(dialog: DialogState): TaskDetailsState = copy(dialog = dialog)

    companion object {
        val INITIAL = State.Loading<TaskDetailsState>()
    }
}