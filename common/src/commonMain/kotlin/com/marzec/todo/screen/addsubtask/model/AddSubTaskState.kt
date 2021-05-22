package com.marzec.todo.screen.addsubtask.model

import com.marzec.todo.model.Task

sealed class AddSubTaskState(
    open val tasks: List<Task>,
) {
    data class Data(
        override val tasks: List<Task>,
    ) : AddSubTaskState(tasks)

    data class Loading(
        override val tasks: List<Task>,
    ) : AddSubTaskState(tasks)

    data class Error(
        override val tasks: List<Task>,
        val message: String
    ) : AddSubTaskState(tasks)

    companion object {
        val INITIAL = Loading(emptyList())
    }
}