package com.marzec.todo.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.TextButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TwoOptionsDialog(state: Dialog.TwoOptionsDialog) {
    if (state.visible) {
        CustomDialog(
            onDismissRequest = { state.onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = state.title,
                        fontSize = 16.sp,
                        style = TextStyle.Default.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(text = state.message)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { state.onConfirm() }) {
                        Text(state.confirmButton)
                    }
                    TextButton(onClick = { state.onDismiss() }) {
                        Text(state.dismissButton)
                    }
                }
            }
        }
    }
}
