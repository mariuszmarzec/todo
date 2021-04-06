package com.marzec.todo.screen.tasks.model

import com.marzec.todo.model.Task

sealed class TasksScreenState {

    data class Data(
        val tasks: List<Task>
    ) : TasksScreenState()

    object Loading : TasksScreenState()

    data class Error(val message: String) : TasksScreenState()

    companion object {

        val INITIAL_STATE = Loading
        val EMPTY_DATA = Data(emptyList())
    }
}