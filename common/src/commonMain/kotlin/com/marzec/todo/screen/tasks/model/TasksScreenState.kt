package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task

data class TasksScreenState(
    val tasks: List<Task>,
    val removeTaskDialog: RemoveDialog
) {
    companion object {
        val INITIAL_STATE = State.Loading<TasksScreenState>()
        val EMPTY_DATA = TasksScreenState(
            tasks = emptyList(),
            removeTaskDialog = RemoveDialog(
                visible = false,
                idToRemove = -1
            )
        )
    }
}

data class RemoveDialog(
    val visible: Boolean = false,
    val idToRemove: Int = -1
)