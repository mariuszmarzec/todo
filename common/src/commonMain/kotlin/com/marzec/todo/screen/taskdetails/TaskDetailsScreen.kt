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
import androidx.compose.ui.unit.sp
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.screen.tasks.model.TasksScreenActions
import com.marzec.todo.view.ActionBar
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import com.marzec.todo.view.TwoOptionsDialog
import kotlinx.coroutines.launch

@Composable
fun TaskDetailsScreen(listId: Int, taskId: Int, navigationStore: NavigationStore, store: TaskDetailsStore) {

    val scope = rememberCoroutineScope()

    val state: TaskDetailsState by store.state.collectAsState()

    LaunchedEffect(listId, taskId) {
        store.init(scope)
        scope.launch { store.initialLoad() }
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
                    Text(text = state.task.description, fontSize = 16.sp)
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
                                    Row(Modifier.fillMaxWidth(fraction = 0.7f)) {
                                        TextListItemView(state = it) {
                                            scope.launch {
                                                store.goToSubtaskDetails(it.id)
                                            }
                                        }
                                    }
                                    TextButton({
                                        scope.launch { store.unpinSubtask(it.id) }
                                    }) {
                                        Text(text = "Unpin")
                                    }
                                }
                            }
                        }
                    }
                }
                TwoOptionsDialog(
                    state = TwoOptionsDialog(
                        title = "Remove task",
                        message = "Do you really want to remove this task?",
                        confirmButton = "Yes",
                        dismissButton = "No",
                        visible = state.removeTaskDialog.visible
                    ),
                    onDismiss = { scope.launch { store.hideRemoveTaskDialog() } },
                    onConfirm = {
                        scope.launch { store.removeTask() }
                    }
                )
            }
            TaskDetailsState.Loading -> {
                Text(text = "Loading")
            }
            is TaskDetailsState.Error -> {
                Text(text = state.message)
            }
        }
    }
}