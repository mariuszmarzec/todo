package com.marzec.todo.screen.lists

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.todo.navigation.model.NavigationActions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.screen.login.model.LoginActions
import kotlinx.coroutines.launch

@Composable
fun ListsScreen(navigationStore: NavigationStore) {
    val scope = rememberCoroutineScope()

    TextButton({ scope.launch { navigationStore.sendAction(NavigationActions.Back) } }) {
        Text("Back")
    }
}