package com.marzec.todo.screen.addnewtask

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.mvi.State
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.screen.addnewtask.model.AddNewTaskActions
import com.marzec.todo.screen.addnewtask.model.AddNewTaskState
import com.marzec.todo.screen.addnewtask.model.AddNewTaskStore
import com.marzec.todo.view.ActionBar
import kotlinx.coroutines.launch

@Composable
fun AddNewTaskScreen(navigationStore: NavigationStore, store: AddNewTaskStore) {

    val scope = rememberCoroutineScope()

    val state: State<AddNewTaskState> by store.state.collectAsState()

    LaunchedEffect(Unit) {
        store.init(scope)
        scope.launch {
            store.initialLoad()
        }
    }

    Scaffold(
        topBar = {
            ActionBar(navigationStore, "Tasks")
        }
    ) {
        when (val state = state) {
            is State.Data -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextField(state.data.description, {
                            scope.launch { store.onDescriptionChanged(it) }
                        })
                    }
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextButton({ scope.launch { store.addNewTask() } }) {
                            state.data.taskId?.let { Text("Update") } ?: Text("Create")
                        }
                    }
                    if (state.data.taskId == null) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            TextButton({ scope.launch { store.addManyTasks() } }) {
                                Text("Create tasks line by line")
                            }
                        }
                    }
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