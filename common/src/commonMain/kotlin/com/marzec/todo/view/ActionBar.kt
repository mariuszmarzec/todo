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
import com.marzec.mvi.Store
import com.marzec.todo.DI
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationAction
import com.marzec.todo.navigation.model.NavigationOptions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.repository.LoginRepository
import kotlinx.coroutines.launch

class ActionBarProvider(private val store: NavigationStore) {
    @Composable
    fun provide(title: String, rightContent: @Composable () -> Unit = { }) {
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

    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton({
            scope.launch { store.goBack() }
        }) {
            Text(text = "Back")
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = title)
        Spacer(modifier = Modifier.weight(1f))

        rightContent()
    }
}