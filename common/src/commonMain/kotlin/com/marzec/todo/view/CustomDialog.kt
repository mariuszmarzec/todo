package com.marzec.todo.view

import androidx.compose.runtime.Composable

@Composable
expect fun CustomDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit)
