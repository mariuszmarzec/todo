package com.marzec.todo.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextListItemView(state: TaskListItem, onClickListener: (TaskListItem) -> Unit) {
    Column (
        modifier = Modifier.fillMaxSize().clickable { onClickListener(state) }
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Text(
                text = state.name,
                fontSize = 16.sp,
                style = TextStyle.Default.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(Modifier.size(16.dp))
        Text(
            text = state.description,
            fontSize = 14.sp,
            style = TextStyle.Default
        )
        Spacer(Modifier.background(Color.Gray).size(1.dp))
    }
}

data class TaskListItem(
    val id: Int,
    val name: String,
    val description: String
)