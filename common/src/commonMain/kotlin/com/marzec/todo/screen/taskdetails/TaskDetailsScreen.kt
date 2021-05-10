package com.marzec.todo.screen.taskdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.screen.taskdetails.model.TaskDetailsActions
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.view.ActionBar
import kotlinx.coroutines.launch

@Composable
fun TaskDetailsScreen(navigationStore: NavigationStore, store: TaskDetailsStore) {

    val scope = rememberCoroutineScope()

    val state: TaskDetailsState by store.state.collectAsState()

    LaunchedEffect(Unit) {
        scope.launch {
            store.sendAction(TaskDetailsActions.InitialLoad)
        }
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
                        TextButton({ scope.launch { store.sendAction(TaskDetailsActions.Edit) } }) {
                            Text("Login")
                        }
                    }
                }
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