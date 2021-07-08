package com.marzec.todo.screen.lists

import com.marzec.todo.model.ToDoList
import com.marzec.todo.view.DialogState

data class ListsScreenState(
    val todoLists: List<ToDoList>,
    val dialog: DialogState
) {

    companion object {
        val INITIAL = ListsScreenState(
            todoLists = emptyList(),
            dialog = DialogState.NoDialog
        )
    }
}
