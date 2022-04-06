package com.marzec.todo.screen.tasks

import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.view.Dialog
import com.marzec.view.DialogBox
import com.marzec.delegate.DialogState
import com.marzec.todo.view.TaskListView
import com.marzec.view.ActionBarProvider
import com.marzec.view.ScreenWithLoading
import com.marzec.view.SearchView
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(store: TasksStore, actionBarProvider: ActionBarProvider) {

    val state: State<TasksScreenState> by store.collectState {
        store.loadList()
    }

    Scaffold(
        topBar = {
            val count = state.data?.tasks?.count()?.let { " ($it)" }.orEmpty()
            actionBarProvider.provide(title = "Tasks$count") {
                when (val state = state) {
                    is State.Data<TasksScreenState> -> {
                        SearchView(state.data.search, store)
                    }
                    else -> Unit
                }
                IconButton({
                    store.onScheduledClick()
                }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Scheduled")
                }

                IconButton({
                    store.logout()
                }) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Logout")
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
        ScreenWithLoading(
            state = state,
            onReloadClick = { store.loadList() }
        ) {
            TaskScreenData(it, store)
        }
    }
}

@Composable
private fun TaskScreenData(
    state: State.Data<TasksScreenState>,
    store: TasksStore
) {
    val scope = rememberCoroutineScope()

    TaskListView(
        tasks = state.data.tasks,
        search = state.data.search.value,
        selected = emptySet(),
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
                            scope.launch { store.removeTask(dialog.idsToRemove) }
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
                        scope.launch { store.removeTask(dialog.idsToRemove) }
                    }
                )
            )
        }
        else -> {
        }
    }
}
