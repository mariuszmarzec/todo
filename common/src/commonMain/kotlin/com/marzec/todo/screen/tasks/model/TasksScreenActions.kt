package com.marzec.todo.screen.tasks.model

sealed class TasksScreenActions {

    data class ListItemClicked(val id: String) : TasksScreenActions()
    object LoadLists : TasksScreenActions()
    object AddNewTask : TasksScreenActions()
}
