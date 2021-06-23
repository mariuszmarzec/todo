package com.marzec.todo.screen.lists

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.mvi.State
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationAction
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.view.TextInputDialog
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import kotlinx.coroutines.launch

@Composable
fun ListsScreen(navigationStore: NavigationStore, listsScreenStore: ListsScreenStore) {
    val scope = rememberCoroutineScope()

    val state: State<ListsScreenState> by listsScreenStore.state.collectAsState()

    LaunchedEffect(Unit) {
        listsScreenStore.init(scope)
        scope.launch {
            listsScreenStore.initialLoad()
        }
    }

    Scaffold(
        topBar = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton({
                    scope.launch { navigationStore.goBack() }
                }) {
                    Text(text = "Back")
                }
                Spacer(modifier = Modifier.width(20.dp))
                Text(text = "ToDo Listy")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch { listsScreenStore.addNewList() }
                }
            ) {
                Text("+")
            }
        }) {
        when (val state = state) {
            is State.Data -> {
                LazyColumn {
                    items(
                        items = state.data.todoLists.map {
                            TextListItem(
                                id = it.id.toString(),
                                name = it.title,
                                description = ""
                            )
                        },
                    ) {
                        key(it.id) {
                            TextListItemView(
                                state = it,
                                onClickListener = {
                                    scope.launch {
                                        navigationStore.next(
                                            NavigationAction(
                                                Destination.Tasks(it.id.toInt())
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                TextInputDialog(
                    state = state.data.addNewListDialog,
                    onTextChanged = {
                        scope.launch { listsScreenStore.onNewListNameChanged(it) }
                    },
                    onConfirm = {
                        scope.launch {
                            listsScreenStore.onCreateButtonClicked(it)                        }
                    },
                    onDismiss = { scope.launch { listsScreenStore.onDialogDismissed() } }
                )
            }
            is State.Loading -> {
                Text(text = "Loading...")
            }
            is State.Error -> {
                Text(text = state.message)
            }
        }
    }
}