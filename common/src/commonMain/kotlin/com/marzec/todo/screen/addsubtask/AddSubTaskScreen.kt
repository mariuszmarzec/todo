package com.marzec.todo.screen.addsubtask

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
import com.marzec.todo.screen.addsubtask.model.AddSubTaskActions
import com.marzec.todo.screen.addsubtask.model.AddSubTaskState
import com.marzec.todo.screen.addsubtask.model.AddSubTaskStore
import com.marzec.todo.view.ActionBar
import kotlinx.coroutines.launch

@Composable
fun AddSubTaskScreen(navigationStore: NavigationStore, store: AddSubTaskStore) {

    val scope = rememberCoroutineScope()

    val state: AddSubTaskState by store.state.collectAsState()

    LaunchedEffect(Unit) {
        scope.launch {
            store.sendAction(AddSubTaskActions.InitialLoad)
        }
    }

    Scaffold(
        topBar = {
            ActionBar(navigationStore, "Add Sub Task")
        }
    ) {
        when (val state = state) {
            is AddSubTaskState.Data -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.size(16.dp))
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextButton({ scope.launch { store.sendAction(AddSubTaskActions.OnAddSubTaskClick) } }) {
                            Text("Create new subtask")
                        }
                    }
                }
            }
            AddSubTaskState.Loading -> {
                Text(text = "Loading")
            }
            is AddSubTaskState.Error -> {
                Text(text = state.message)
            }
        }
    }
}