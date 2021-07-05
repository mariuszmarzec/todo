package com.marzec.todo.screen.lists

import com.marzec.todo.model.ToDoList

data class ListsScreenState(
    val todoLists: List<ToDoList>,
    val dialog: ListsScreenDialog?
) {

    companion object {
        val INITIAL = ListsScreenState(
            todoLists = emptyList(),
            dialog = null
        )
    }
}

sealed class ListsScreenDialog {

    data class RemoveListDialog(
        val id: Int,
    ): ListsScreenDialog()

    data class AddNewListDialog(
        val inputField: String,
    ): ListsScreenDialog()
}
