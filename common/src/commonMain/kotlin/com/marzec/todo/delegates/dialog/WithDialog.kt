package com.marzec.todo.delegates.dialog

import com.marzec.todo.view.DialogState

interface WithDialog<T> {

    val dialog: DialogState

    fun copyWithDialog(dialog: DialogState): T
}