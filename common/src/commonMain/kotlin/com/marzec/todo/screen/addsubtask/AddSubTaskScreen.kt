package com.marzec.todo.screen.addsubtask

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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.screen.addsubtask.model.AddSubTaskState
import com.marzec.todo.screen.addsubtask.model.AddSubTaskStore
import com.marzec.todo.view.ActionBar
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import kotlinx.coroutines.launch

@Composable
fun AddSubTaskScreen(navigationStore: NavigationStore, store: AddSubTaskStore) {

    val scope = rememberCoroutineScope()

    val state: AddSubTaskState by store.state.collectAsState()

    LaunchedEffect(Unit) {
        store.init(scope)
        store.initialLoad()
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
                        TextButton({ scope.launch { store.onAddSubTaskClick() } }) {
                            Text("Create new subtask")
                        }
                    }
                    Spacer(Modifier.size(16.dp))
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
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(Modifier.fillMaxWidth(fraction = 0.7f)) {
                                        TextListItemView(state = it,
                                            onClickListener = {
                                                scope.launch {
                                                    store.goToSubtaskDetails(it.id)
                                                }
                                            }) {
                                            IconButton({
                                                scope.launch { store.pinSubtask(it.id) }
                                            }) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Pin")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
            is AddSubTaskState.Loading -> {
                Text(text = "Loading")
            }
            is AddSubTaskState.Error -> {
                Text(text = state.message)
            }
        }
    }
}