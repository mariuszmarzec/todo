package com.marzec.todo.screen.lists

data class ListsScreenState(
    val todoLists: List<String>,
    val addNewListDialog: Boolean
)