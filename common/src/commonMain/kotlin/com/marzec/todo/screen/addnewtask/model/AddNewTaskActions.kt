package com.marzec.todo.screen.addnewtask.model

sealed class AddNewTaskActions {
    object Add : AddNewTaskActions()
    data class DescriptionChanged(val description: String) : AddNewTaskActions()
}
