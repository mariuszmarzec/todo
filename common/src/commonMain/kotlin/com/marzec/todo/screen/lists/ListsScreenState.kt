package com.marzec.todo.screen.lists

import com.marzec.todo.model.ToDoList
import com.marzec.todo.view.TextInputDialog

sealed class ListsScreenState {

    data class Data(
        val todoLists: List<ToDoList>,
        val addNewListDialog: TextInputDialog
    ) : ListsScreenState()

    data class Error(val message: String) : ListsScreenState()

    companion object {
        val INITIAL = Data(
            todoLists = emptyList(),
            addNewListDialog = TextInputDialog(
                visible = false,
                title = "Put name of new list",
                inputField = "",
                confirmButton = "Create",
                dismissButton = "Cancel"
            )
        )
    }
}