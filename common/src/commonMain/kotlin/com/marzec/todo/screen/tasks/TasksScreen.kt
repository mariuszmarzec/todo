package com.marzec.todo.screen.tasks

import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.mvi.State
import com.marzec.todo.extensions.EMPTY_STRING
import com.marzec.todo.extensions.collectState
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.todo.view.ActionBarProvider
import com.marzec.todo.view.Dialog
import com.marzec.todo.view.DialogBox
import com.marzec.todo.view.DialogState
import com.marzec.todo.view.SearchView
import com.marzec.todo.view.TaskListView
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(store: TasksStore, actionBarProvider: ActionBarProvider) {

    val state: State<TasksScreenState> by store.collectState {
        store.loadList()
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide(title = "Tasks") {
                when (val state = state) {
                    is State.Data<TasksScreenState> -> {
                        SearchView(state.data.search, store)
                    }
                    else -> Unit
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    store.addNewTask()
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add new")
            }
        }) {
        when (val state = state) {
            is State.Data -> {
                TaskScreenData(state, store)
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
private fun TaskScreenData(
    state: State.Data<TasksScreenState>,
    store: TasksStore
) {
    val scope = rememberCoroutineScope()
    val searchQuery = state.data.search.value.trim().split(" ")

    TaskListView(
        tasks = state.data.tasks.filter { task ->
            searchQuery == listOf(EMPTY_STRING) || searchQuery.all {
                task.description.contains(
                    it,
                    ignoreCase = true
                )
            }
        },
        showButtonsInColumns = false,
        onClickListener = {
            store.onListItemClicked(it)
        },
        onOpenUrlClick = { store.openUrl(it) },
        onMoveToTopClick = { store.moveToTop(it) },
        onMoveToBottomClick = {
            store.moveToBottom(it)
        },
        onRemoveButtonClick = {
            store.onRemoveButtonClick(it)
        },
        onPinButtonClick = null
    )

    val dialog = state.data.dialog
    when (dialog) {
        is DialogState.RemoveDialogWithCheckBox -> {
            DialogBox(
                state = Dialog.TwoOptionsDialogWithCheckbox(
                    twoOptionsDialog = Dialog.TwoOptionsDialog(
                        title = "Remove task",
                        message = "Do you really want to remove this task?",
                        confirmButton = "Yes",
                        dismissButton = "No",
                        onDismiss = { scope.launch { store.closeDialog() } },
                        onConfirm = {
                            scope.launch { store.removeTask(dialog.idToRemove) }
                        }
                    ),
                    checked = dialog.checked,
                    checkBoxLabel = "Remove with all sub-tasks",
                    onCheckedChange = {
                        scope.launch { store.onRemoveWithSubTasksChange() }
                    }
                )
            )
        }
        is DialogState.RemoveDialog -> {
            DialogBox(
                state = Dialog.TwoOptionsDialog(
                    title = "Remove task",
                    message = "Do you really want to remove this task?",
                    confirmButton = "Yes",
                    dismissButton = "No",
                    onDismiss = { scope.launch { store.closeDialog() } },
                    onConfirm = {
                        scope.launch { store.removeTask(dialog.idToRemove) }
                    }
                )
            )
        }
        else -> {
        }
    }
}
