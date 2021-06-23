package com.marzec.todo.screen.lists

import com.marzec.todo.model.ToDoList
import com.marzec.todo.view.TextInputDialog

data class ListsScreenState(
    val todoLists: List<ToDoList>,
    val addNewListDialog: TextInputDialog
) {

    companion object {
        val INITIAL = ListsScreenState(
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