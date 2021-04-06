package com.marzec.todo.screen.lists

sealed class ListScreenActions {
    data class NewListNameChanged(val text: String) : ListScreenActions()
    data class CreateButtonClicked(val newListName: String) : ListScreenActions()
    object DialogDismissed : ListScreenActions()
    object AddNewList : ListScreenActions()
    object LoadLists : ListScreenActions()
}