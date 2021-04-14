package com.marzec.todo.screen.addnewtask.model

sealed class AddNewTaskState {

    data class Data(
        val description: String
    ) : AddNewTaskState()

    object Loading : AddNewTaskState()

    data class Error(val message: String) : AddNewTaskState()

    companion object {
        val DEFAULT = Data(description = "")
    }
}
