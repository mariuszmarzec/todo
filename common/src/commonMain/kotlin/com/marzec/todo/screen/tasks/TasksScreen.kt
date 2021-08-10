package com.marzec.todo.screen.tasks

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.marzec.mvi.State
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.todo.view.ActionBarProvider
import com.marzec.todo.view.Dialog
import com.marzec.todo.view.DialogBox
import com.marzec.todo.view.DialogState
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(tasksStore: TasksStore, actionBarProvider: ActionBarProvider) {
    val scope = rememberCoroutineScope()

    val state: State<TasksScreenState> by tasksStore.state.collectAsState()

    LaunchedEffect(Unit) {
        tasksStore.init(this)
        tasksStore.loadList()
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide(title = "Tasks")
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch { tasksStore.addNewTask() }
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add new")
            }
        }) {
        when (val state = state) {
            is State.Data -> {
                LazyColumn {
                    items(
                        items = state.data.tasks.map {
                            TextListItem(
                                id = it.id.toString(),
                                name = it.description.lines().first(),
                                description = it.subTasks.firstOrNull()?.description?.lines()?.first() ?: ""
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
                                    IconButton({
                                        scope.launch { tasksStore.moveToTop(it.id) }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move to top"
                                        )
                                    }
                                    IconButton({
                                        scope.launch { tasksStore.moveToBottom(it.id) }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move to bottom"
                                        )
                                    }
                                    IconButton({
                                        scope.launch { tasksStore.showRemoveDialog(it.id) }
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
                val removeTaskDialog = state.data.removeTaskDialog
                if (removeTaskDialog is DialogState.RemoveDialog) {
                    DialogBox(
                        state = Dialog.TwoOptionsDialog(
                            title = "Remove task",
                            message = "Do you really want to remove this task?",
                            confirmButton = "Yes",
                            dismissButton = "No",
                            onDismiss = { scope.launch { tasksStore.hideRemoveDialog() } },
                            onConfirm = {
                                scope.launch {
                                    tasksStore.removeTask(removeTaskDialog.idToRemove)
                                }
                            }
                        )
                    )

                }
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