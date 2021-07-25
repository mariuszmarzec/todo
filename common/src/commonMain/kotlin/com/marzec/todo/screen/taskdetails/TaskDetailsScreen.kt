package com.marzec.todo.screen.taskdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.view.ActionBar
import com.marzec.todo.view.Dialog
import com.marzec.todo.view.DialogBox
import com.marzec.todo.view.DialogState
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import kotlinx.coroutines.launch

@Composable
fun TaskDetailsScreen(listId: Int, taskId: Int, navigationStore: NavigationStore, store: TaskDetailsStore) {

    val scope = rememberCoroutineScope()

    val state: TaskDetailsState by store.state.collectAsState()

    LaunchedEffect(listId, taskId) {
        store.init(scope)
        scope.launch { store.loadDetails() }
    }

    Scaffold(
        topBar = {
            ActionBar(navigationStore, "Task details")
        }
    ) {
        when (val state = state) {
            is TaskDetailsState.Data -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    SelectionContainer {
                        Text(text = state.task.description, fontSize = 16.sp)
                    }
                    Spacer(Modifier.size(16.dp))
                    IconButton({
                        scope.launch { store.copyDescription() }
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Copy")
                    }
                    Spacer(Modifier.size(16.dp))
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextButton({ scope.launch { store.edit() } }) {
                            Text("Edit")
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                    TextButton({
                        scope.launch { store.showRemoveTaskDialog() }
                    }) {
                        Text(text = "Remove")
                    }
                    Spacer(Modifier.size(16.dp))
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextButton({ scope.launch { store.addSubTask() } }) {
                            Text("Add subtask")
                        }
                    }

                    Spacer(Modifier.size(16.dp))
                    LazyColumn {
                        items(
                            items = state.task.subTasks.map {
                                TextListItem(
                                    id = it.id.toString(),
                                    name = it.description,
                                    description = it.subTasks.firstOrNull()?.description ?: ""
                                )
                            },
                        ) {
                            key(it.id) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    TextListItemView(
                                        state = it,
                                        onClickListener = {
                                            scope.launch {
                                                store.goToSubtaskDetails(it.id)
                                            }
                                        }
                                    ) {
                                        IconButton({
                                            scope.launch { store.moveToTop(it.id) }
                                        }) {
                                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Move to top")
                                        }
                                        IconButton({
                                            scope.launch { store.moveToBottom(it.id) }
                                        }) {
                                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Move to bottom")
                                        }
                                        IconButton({
                                            scope.launch { store.unpinSubtask(it.id) }
                                        }) {
                                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Unpin")
                                        }
                                        IconButton({
                                            scope.launch { store.showRemoveSubTaskDialog(it.id) }
                                        }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                val dialog = state.dialog
                if (dialog is DialogState.RemoveDialog) {
                    DialogBox(
                        state = Dialog.TwoOptionsDialog(
                            title = "Remove task",
                            message = "Do you really want to remove this task?",
                            confirmButton = "Yes",
                            dismissButton = "No",
                            onDismiss = { scope.launch { store.hideRemoveTaskDialog() } },
                            onConfirm = {
                                scope.launch { store.removeTask(dialog.idToRemove) }
                            }
                        )
                    )

                }
            }
            is TaskDetailsState.Loading -> {
                Text(text = "Loading")
            }
            is TaskDetailsState.Error -> {
                Text(text = state.message)
            }
        }
    }
}