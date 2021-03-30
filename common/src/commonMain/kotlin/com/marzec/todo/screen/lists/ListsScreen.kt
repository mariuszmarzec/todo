package com.marzec.todo.screen.lists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.marzec.todo.navigation.model.NavigationActions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import kotlinx.coroutines.launch

@Composable
fun ListsScreen(navigationStore: NavigationStore, listsScreenStore: ListsScreenStore) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Box(contentAlignment = Alignment.CenterStart) {
                TextButton( {
                    scope.launch { navigationStore.sendAction(NavigationActions.Back) }
                }) {
                    Text(text = "Back")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch { listsScreenStore.sendAction(ListScreenActions.AddNewList) }
                }
            ) {
                Text("+")
            }
        }, bodyContent = {
            LazyColumn {
                listOf(
                    TextListItem("1", "first", ""),
                    TextListItem("2", "second", "")
                ).forEach {
                    item { TextListItemView(it) {} }
                }
            }
        }
    )
}