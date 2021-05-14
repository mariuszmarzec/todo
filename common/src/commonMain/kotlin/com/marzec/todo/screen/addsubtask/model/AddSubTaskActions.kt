package com.marzec.todo.screen.addsubtask.model

sealed class AddSubTaskActions {
    object InitialLoad: AddSubTaskActions()
    object OnAddSubTaskClick : AddSubTaskActions()
}
