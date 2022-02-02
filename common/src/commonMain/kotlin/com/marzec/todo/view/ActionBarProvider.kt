package com.marzec.todo.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.todo.navigation.model.NavigationStore
import kotlinx.coroutines.launch

class ActionBarProvider(private val store: NavigationStore) {
    @Composable
    fun provide(title: String = "", rightContent: @Composable () -> Unit = { }) {
        ActionBar(store = store, title = title, rightContent)
    }
}

@Composable
private fun ActionBar(
    store: NavigationStore,
    title: String,
    rightContent: @Composable () -> Unit = { }
) {
    val scope = rememberCoroutineScope()

    val state by store.state.collectAsState()

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.backStack.size > 1) {
            IconButton({
                scope.launch { store.goBack() }
            }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title)

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            rightContent()
        }
    }
}
