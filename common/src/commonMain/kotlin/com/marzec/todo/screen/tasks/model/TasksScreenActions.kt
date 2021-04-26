package com.marzec.todo.screen.tasks.model

sealed class TasksScreenActions {

    data class ListItemClicked(val id: String) : TasksScreenActions()
    data class RemoveTask(val id: Int) : TasksScreenActions()
    data class ShowRemoveDialog(val id: Int) : TasksScreenActions()
    object LoadLists : TasksScreenActions()
    object AddNewTask : TasksScreenActions()
    object HideRemoveDialog : TasksScreenActions()
}
