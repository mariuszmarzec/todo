package com.marzec.todo.screen.addsubtask.model

import com.marzec.mvi.State
import com.marzec.todo.model.Task

data class AddSubTaskData(
    val tasks: List<Task>,
) {
    companion object {
        val DEFAULT = AddSubTaskData(emptyList())
        val INITIAL = State.Loading(DEFAULT)
    }
}
