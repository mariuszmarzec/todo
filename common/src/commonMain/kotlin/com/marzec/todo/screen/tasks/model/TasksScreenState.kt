package com.marzec.todo.screen.tasks.model

import com.marzec.todo.model.Task

sealed class TasksScreenState(
    open val tasks: List<Task>
) {
    data class Data(
        override val tasks: List<Task>,
        val removeTaskDialog: RemoveDialog
    ) : TasksScreenState(tasks)

    class Loading(
        override val tasks: List<Task>
    ) : TasksScreenState(tasks)

    data class Error(
        override val tasks: List<Task>,
        val message: String
    ) : TasksScreenState(tasks)

    companion object {

        val INITIAL_STATE = Loading(emptyList())
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