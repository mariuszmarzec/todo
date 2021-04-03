package com.marzec.todo.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TextInputDialog(
    state: TextInputDialog,
    onTextChanged: (String) -> Unit = {},
    onDismiss: () -> Unit = {},
    onConfirm: (String) -> Unit = {}
) {
    if (state.visible) {
        Dialog(
            onDismissRequest = { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.title)

                Box(modifier = Modifier.padding(16.dp)) {
                    TextField(state.inputField, {
                        onTextChanged(it)
                    })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { onConfirm(state.inputField) }) {
                        Text(state.confirmButton)
                    }
                    TextButton(onClick = { onDismiss() }) {
                        Text(state.dismissButton)
                    }
                }
            }
        }
    }
}

data class TextInputDialog(
    val title: String,
    val inputField: String,
    val confirmButton: String,
    val dismissButton: String,
    val visible: Boolean
)