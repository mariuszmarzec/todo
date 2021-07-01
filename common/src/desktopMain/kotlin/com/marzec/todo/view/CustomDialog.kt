package com.marzec.todo.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.v1.Dialog

@Composable
actual fun CustomDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        content()
    }
}