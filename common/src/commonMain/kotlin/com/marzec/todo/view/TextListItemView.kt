package com.marzec.todo.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TextListItemView(state: TextListItem, onClickListener: (TextListItem) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clickable { onClickListener(state) }) {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = state.name,
                fontSize = 16.sp,
                style = TextStyle.Default.copy(fontWeight = FontWeight.Bold)
            )
            if (state.description.isNotEmpty()) {
                Spacer(Modifier.size(16.dp))
                Text(
                    text = state.description,
                    fontSize = 14.sp,
                    style = TextStyle.Default
                )
            }
        }
    }
}

data class TextListItem(
    val id: String,
    val name: String,
    val description: String
)