package com.marzec.todo.screen.addnewtask

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.time.formatDate
import com.marzec.todo.model.Scheduler
import com.marzec.todo.screen.addnewtask.model.AddNewTaskState
import com.marzec.todo.screen.addnewtask.model.AddNewTaskStore
import com.marzec.view.ActionBarProvider

@Composable
fun AddNewTaskScreen(
    store: AddNewTaskStore,
    actionBarProvider: ActionBarProvider
) {

    val state: State<AddNewTaskState> by store.collectState {
        store.initialLoad()
        store.onSchedulerRequest()
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide("Tasks")
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
                            store.onDescriptionChanged(it)
                        })
                    }
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.data.taskId == null) {
                            Checkbox(
                                checked = state.data.highestPriorityAsDefault,
                                onCheckedChange = { store.toggleHighestPriority() }
                            )
                            TextButton({ store.toggleHighestPriority() }) {
                                Text("Highest priority as default")
                            }
                        }
                    }
                    Box(modifier = Modifier.padding(16.dp)) {
                        TextButton({ store.addNewTask() }) {
                            Text(text = state.data.taskId?.let { "Update" } ?: "Create")
                        }
                    }
                    if (state.data.taskId == null) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            TextButton({ store.addManyTasks() }) {
                                Text("Create tasks line by line")
                            }
                        }
                    }
                    if (state.data.parentTaskId == null) {
                        ScheduleRow(
                            scheduler = state.data.scheduler,
                            onScheduleButtonClick = { store.onScheduleButtonClick() },
                            onRemoveSchedulerButtonClick = { store.onRemoveSchedulerButtonClick() }
                        )
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

@Composable
fun ScheduleRow(
    scheduler: Scheduler?,
    onScheduleButtonClick: () -> Unit,
    onRemoveSchedulerButtonClick: () -> Unit
) {
    if (scheduler != null) {
        Row(
            modifier = Modifier.clickable { onScheduleButtonClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton({ onRemoveSchedulerButtonClick() }) {
                Icon(Icons.Default.Clear, "Remove scheduler")
            }
            Text(
                text = scheduler.description
            )
        }
    } else {
        Button(onClick = {
            onScheduleButtonClick()
        }) {
            Text("Schedule")
        }
    }
}

private val Scheduler.description: String
    get() = when (this) {
        is Scheduler.OneShot -> {
            this::class.simpleName.orEmpty() +
                    String.format(" at %02d:%02d", hour, minute) +
                    " from ${startDate.formatDate("dd-MM-yyyy")}"
        }
        is Scheduler.Weekly -> {
            this::class.simpleName.orEmpty() +
                    String.format(" at %02d:%02d", hour, minute) +
                    " from ${startDate.formatDate("dd-MM-yyyy")}" +
                    " days: $daysOfWeek" +
                    " repeat: $repeatCount times in every $repeatInEveryPeriod week"
        }
        is Scheduler.Monthly -> {
            this::class.simpleName.orEmpty() +
                    String.format(" at %02d:%02d", hour, minute) +
                    " from ${startDate.formatDate("dd-MM-yyyy")}" +
                    " day of month $dayOfMonth" +
                    " repeat: $repeatCount times in every $repeatInEveryPeriod month"
        }
    }
