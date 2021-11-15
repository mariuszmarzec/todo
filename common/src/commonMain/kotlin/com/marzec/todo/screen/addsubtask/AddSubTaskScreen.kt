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
import com.marzec.mvi.State
import com.marzec.todo.screen.addsubtask.model.AddSubTaskData
import com.marzec.todo.screen.addsubtask.model.AddSubTaskStore
import com.marzec.todo.view.ActionBarProvider
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView

@Composable
fun AddSubTaskScreen(
    store: AddSubTaskStore,
    actionBarProvider: ActionBarProvider
) {

    val scope = rememberCoroutineScope()

    val state: State<AddSubTaskData> by store.state.collectAsState()

    LaunchedEffect(Unit) {
        store.init(scope) {
            store.initialLoad()
        }
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide("Add Sub Task")
        }
    ) {
        when (val state = state) {
            is State.Data -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(Modifier.size(16.dp))
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextButton({ store.onAddSubTaskClick() }) {
                            Text("Create new subtask")
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                    LazyColumn {
                        items(
                            items = state.data.tasks.map {
                                TextListItem(
                                    id = it.id.toString(),
                                    name = it.description.lines().first(),
                                    description = it.subTasks.firstOrNull()?.description?.lines()?.first() ?: ""
                                )
                            },
                        ) { item ->
                            key(item.id) {
                                Row(Modifier.fillMaxWidth()) {
                                    TextListItemView(state = item,
                                        onClickListener = {
                                            store.goToSubtaskDetails(it.id)
                                        }) {
                                        IconButton({
                                            store.pinSubtask(item.id)
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
            is State.Loading -> {
                Text(text = "Loading")
            }
            is State.Error -> {
                Text(text = state.message)
            }
        }
    }
}
