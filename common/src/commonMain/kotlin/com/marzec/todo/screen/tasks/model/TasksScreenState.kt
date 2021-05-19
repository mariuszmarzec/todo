package com.marzec.todo.screen.tasks.model

import com.marzec.todo.model.Task

sealed class TasksScreenState {

    data class Data(
        val tasks: List<Task>,
        val removeTaskDialog: RemoveDialog
    ) : TasksScreenState()

    object Loading : TasksScreenState()

    data class Error(val message: String) : TasksScreenState()

    companion object {

        val INITIAL_STATE = Loading
        val EMPTY_DATA = Data(
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