package com.marzec.todo.screen.addnewtask.model

sealed class AddNewTaskActions {
    object Add : AddNewTaskActions()
    object InitialLoad : AddNewTaskActions()
    data class DescriptionChanged(val description: String) : AddNewTaskActions()
}
