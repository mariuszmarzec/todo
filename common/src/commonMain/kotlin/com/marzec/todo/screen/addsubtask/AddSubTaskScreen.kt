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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marzec.mvi.State
import com.marzec.todo.extensions.EMPTY_STRING
import com.marzec.todo.extensions.collectState
import com.marzec.todo.screen.addsubtask.model.AddSubTaskData
import com.marzec.todo.screen.addsubtask.model.AddSubTaskStore
import com.marzec.todo.view.ActionBarProvider
import com.marzec.todo.view.TaskListView
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView

@Composable
fun AddSubTaskScreen(
    store: AddSubTaskStore,
    actionBarProvider: ActionBarProvider
) {

    val state: State<AddSubTaskData> by store.collectState {
        store.initialLoad()
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
                    TaskListView(
                        tasks = state.data.tasks,
                        showButtonsInColumns = false,
                        onClickListener = { },
                        onUnpinButtonClick = {
                            store.pinSubtask(it)
                        }
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
