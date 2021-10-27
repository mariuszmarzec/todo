package com.marzec.todo.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog

@Composable
actual fun CustomDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onCloseRequest = onDismissRequest) {
        content()
    }
}
