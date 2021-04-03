package com.marzec.todo.screen.lists

sealed class ListScreenActions {
    data class ListItemClicked(val id: String) : ListScreenActions()
    object AddNewList : ListScreenActions()
    object LoadLists : ListScreenActions()
}