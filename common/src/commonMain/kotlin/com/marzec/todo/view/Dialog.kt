package com.marzec.todo.view

import androidx.compose.runtime.Composable


@Composable
fun DialogBox(
    state: Dialog
) {
    if (state.visible) {
        when (state) {
            is Dialog.TextInputDialog -> TextInputDialog(state)
            is Dialog.TwoOptionsDialog -> TwoOptionsDialog(state)
        }
    }
}

sealed class Dialog(
    open val visible: Boolean
) {

    data class TextInputDialog(
        val title: String,
        val inputField: String,
        val confirmButton: String,
        val dismissButton: String,
        val onTextChanged: (String) -> Unit = {},
        val onDismiss: () -> Unit = {},
        val onConfirm: (String) -> Unit = {},
        override val visible: Boolean
    ) : Dialog(visible)

    data class TwoOptionsDialog(
        val title: String,
        val message: String,
        val confirmButton: String,
        val dismissButton: String,
        val onDismiss: () -> Unit = {},
        val onConfirm: () -> Unit = {},
        override val visible: Boolean
    ) : Dialog(visible)
}