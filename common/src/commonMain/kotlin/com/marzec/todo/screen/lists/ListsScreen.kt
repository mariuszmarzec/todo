package com.marzec.todo.screen.lists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.mvi.State
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationAction
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.view.ActionBarProvider
import com.marzec.todo.view.Dialog
import com.marzec.todo.view.DialogBox
import com.marzec.todo.view.DialogState
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView

@Composable
fun ListsScreen(
    navigationStore: NavigationStore,
    actionBarProvider: ActionBarProvider,
    listsScreenStore: ListsScreenStore
) {
    val scope = rememberCoroutineScope()

    val state: State<ListsScreenState> by listsScreenStore.state.collectAsState()

    LaunchedEffect(Unit) {
        listsScreenStore.init(scope) {
            listsScreenStore.initialLoad()
        }
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide(title = "ToDo Listy") {
                IconButton({
                    listsScreenStore.logout()
                }) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Logout")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    listsScreenStore.addNewList()
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add new")
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
                    ) { item ->
                        key(item.id) {
                            TextListItemView(
                                state = item,
                                onClickListener = {
                                    navigationStore.next(
                                        NavigationAction(
                                            Destination.Tasks(it.id.toInt())
                                        )
                                    )
                                }
                            ) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    IconButton({
                                        listsScreenStore.showRemoveListDialog(item.id.toInt())
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val dialog = when (val dialogState = state.data.dialog) {
                    is DialogState.RemoveDialog -> {
                        Dialog.TwoOptionsDialog(
                            title = "Remove List",
                            message = "Do you really want to remove list?",
                            confirmButton = "Remove",
                            dismissButton = "Cancel",
                            onConfirm = {
                                listsScreenStore.removeList(dialogState.idToRemove)
                            },
                            onDismiss = { listsScreenStore.closeDialog() }
                        )
                    }
                    is DialogState.InputDialog -> {
                        Dialog.TextInputDialog(
                            title = "Put name of new list",
                            inputField = dialogState.inputField,
                            confirmButton = "Create",
                            dismissButton = "Cancel",
                            onTextChanged = {
                                listsScreenStore.onNewListNameChanged(it)
                            },
                            onConfirm = {
                                listsScreenStore.onCreateButtonClicked(it)
                            },
                            onDismiss = { listsScreenStore.closeDialog() }
                        )
                    }
                    else -> Dialog.NoDialog
                }
                DialogBox(state = dialog)
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
