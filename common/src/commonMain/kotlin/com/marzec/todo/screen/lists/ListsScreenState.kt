package com.marzec.todo.screen.lists

import com.marzec.todo.model.ToDoList

sealed class ListsScreenState {

    data class Data(
        val todoLists: List<ToDoList>,
        val addNewListDialog: Boolean
    ) : ListsScreenState()

    data class Error(val message: String) : ListsScreenState()

    companion object {
        val INITIAL = Data(emptyList(), false)
    }
}