package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task

data class TasksScreenState(
    val tasks: List<Task>,
    val removeTaskDialog: RemoveDialog?
) {
    companion object {
        val INITIAL_STATE = State.Loading<TasksScreenState>()
        val EMPTY_DATA = TasksScreenState(
            tasks = emptyList(),
            removeTaskDialog = null
        )
    }
}

data class RemoveDialog(
    val idToRemove: Int = -1
)