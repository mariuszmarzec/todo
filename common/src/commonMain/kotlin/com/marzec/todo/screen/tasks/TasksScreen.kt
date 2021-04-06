package com.marzec.todo.screen.tasks

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
import com.marzec.todo.navigation.model.NavigationActions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.screen.tasks.model.TasksScreenActions
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(navigationStore: NavigationStore, tasksStore: TasksStore) {
    val scope = rememberCoroutineScope()

    val state: TasksScreenState by tasksStore.state.collectAsState()

    LaunchedEffect(Unit) {
        scope.launch {
            tasksStore.sendAction(TasksScreenActions.LoadLists)
        }
    }

    Scaffold(
        topBar = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton({
                    scope.launch { navigationStore.sendAction(NavigationActions.Back) }
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
                    scope.launch { tasksStore.sendAction(TasksScreenActions.AddNewTask) }
                }
            ) {
                Text("+")
            }
        }) {
        when (val state = state) {
            is TasksScreenState.Data -> {
                LazyColumn {
                    items(
                        items = state.tasks.map {
                            TextListItem(
                                id = it.id.toString(),
                                name = it.description,
                                description = it.subTasks.firstOrNull()?.description ?: ""
                            )
                        },
                    ) {
                        key(it.id) {
                            TextListItemView(state = it) {
                                scope.launch {
                                    tasksStore.sendAction(
                                        TasksScreenActions.ListItemClicked(
                                            it.id
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            TasksScreenState.Loading -> {
                Text(text = "Loading")
            }
            is TasksScreenState.Error -> {
                Text(text = state.message)
            }
        }
    }
}