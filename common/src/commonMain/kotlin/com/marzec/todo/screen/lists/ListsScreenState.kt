package com.marzec.todo.screen.lists

import com.marzec.todo.model.ToDoList

data class ListsScreenState(
    val todoLists: List<ToDoList>,
    val addNewListDialog: NewListDialog
) {

    companion object {
        val INITIAL = ListsScreenState(
            todoLists = emptyList(),
            addNewListDialog = NewListDialog(
                visible = false,
                inputField = ""
            )
        )
    }
}

data class NewListDialog(
    val visible: Boolean,
    val inputField: String,
)