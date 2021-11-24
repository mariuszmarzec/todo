package com.marzec.todo.screen.lists

import com.marzec.todo.delegates.dialog.WithDialog
import com.marzec.todo.model.ToDoList
import com.marzec.todo.view.DialogState

data class ListsScreenState(
    val todoLists: List<ToDoList>,
    override val dialog: DialogState
) : WithDialog<ListsScreenState> {

    override fun copyWithDialog(dialog: DialogState): ListsScreenState = copy(dialog = dialog)

    companion object {
        val INITIAL = ListsScreenState(
            todoLists = emptyList(),
            dialog = DialogState.NoDialog
        )
    }
}
