package com.marzec.todo.screen.addsubtask.model

import com.marzec.todo.model.Task

sealed class AddSubTaskState {
    data class Data(
        val tasks: List<Task>,
    ) : AddSubTaskState()
    object Loading : AddSubTaskState()
    data class Error(val message: String) : AddSubTaskState()

    companion object {
        val INITIAL = Loading
    }
}