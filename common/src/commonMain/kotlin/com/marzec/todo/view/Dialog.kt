package com.marzec.todo.view

import androidx.compose.runtime.Composable


@Composable
fun DialogBox(
    state: Dialog
) {
    when (state) {
        is Dialog.TextInputDialog -> TextInputDialog(state)
        is Dialog.TwoOptionsDialog -> TwoOptionsDialog(state)
        is Dialog.NoDialog -> Unit
    }
}

sealed class Dialog {

    data class TextInputDialog(
        val title: String,
        val inputField: String,
        val confirmButton: String,
        val dismissButton: String,
        val onTextChanged: (String) -> Unit = {},
        val onDismiss: () -> Unit = {},
        val onConfirm: (String) -> Unit = {},
    ) : Dialog()

    data class TwoOptionsDialog(
        val title: String,
        val message: String,
        val confirmButton: String,
        val dismissButton: String,
        val onDismiss: () -> Unit = {},
        val onConfirm: () -> Unit = {},
    ) : Dialog()

    object NoDialog: Dialog()
}

sealed class DialogState {

    data class RemoveDialog(
        val idToRemove: Int,
    ): DialogState()

    data class InputDialog(
        val inputField: String,
    ): DialogState()

    object NoDialog: DialogState()
}
