package com.marzec.todo.screen.tasks

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.todo.view.Dialog
import com.marzec.todo.view.DialogBox
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(navigationStore: NavigationStore, tasksStore: TasksStore) {
    val scope = rememberCoroutineScope()

    val state: State<TasksScreenState> by tasksStore.state.collectAsState()

    LaunchedEffect(Unit) {
        tasksStore.init(this)
        tasksStore.loadList()
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
                Text(text = "Tasks")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch { tasksStore.addNewTask() }
                }
            ) {
                Text("+")
            }
        }) {
        when (val state = state) {
            is State.Data -> {
                LazyColumn {
                    items(
                        items = state.data.tasks.map {
                            TextListItem(
                                id = it.id.toString(),
                                name = it.description,
                                description = it.subTasks.firstOrNull()?.description ?: ""
                            )
                        },
                    ) {
                        key(it.id) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextListItemView(state = it, onClickListener = {
                                    scope.launch {
                                        tasksStore.onListItemClicked(it.id)
                                    }
                                }) {
                                    TextButton({
                                        scope.launch { tasksStore.moveToTop(it.id) }
                                    }) {
                                        Text(text = "Move to top")
                                    }
                                    TextButton({
                                        scope.launch { tasksStore.moveToBottom(it.id) }
                                    }) {
                                        Text(text = "Move to bottom")
                                    }
                                    TextButton({
                                        scope.launch { tasksStore.showRemoveDialog(it.id) }
                                    }) {
                                        Text(text = "Remove")
                                    }
                                }
                            }
                        }
                    }
                }
                DialogBox(
                    state = Dialog.TwoOptionsDialog(
                        title = "Remove task",
                        message = "Do you really want to remove this task?",
                        confirmButton = "Yes",
                        dismissButton = "No",
                        visible = state.data.removeTaskDialog.visible,
                        onDismiss = { scope.launch { tasksStore.hideRemoveDialog() } },
                        onConfirm = {
                            scope.launch {
                                tasksStore.removeTask(state.data.removeTaskDialog.idToRemove)
                            }
                        }
                    )
                )
            }
            is State.Loading -> {
                Text(text = "Loading")
            }
            is State.Error -> {
                Text(text = state.message)
            }
        }
    }
}