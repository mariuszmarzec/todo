package com.marzec.todo.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.todo.navigation.model.NavigationActions
import com.marzec.todo.navigation.model.NavigationStore
import kotlinx.coroutines.launch

@Composable
fun ActionBar(store: NavigationStore, title: String) {
    val scope = rememberCoroutineScope()
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton({
            scope.launch { store.sendAction(NavigationActions.Back) }
        }) {
            Text(text = "Back")
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = title)
    }
}