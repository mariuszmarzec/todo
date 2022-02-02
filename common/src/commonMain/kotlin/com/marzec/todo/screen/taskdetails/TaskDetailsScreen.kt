package com.marzec.todo.screen.taskdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Checkbox
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MenuDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.mvi.State
import com.marzec.todo.extensions.EMPTY_STRING
import com.marzec.todo.extensions.collectState
import com.marzec.todo.extensions.urls
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.view.ActionBarProvider
import com.marzec.todo.view.Dialog
import com.marzec.todo.view.DialogBox
import com.marzec.todo.view.DialogState
import com.marzec.todo.view.SearchView
import com.marzec.todo.view.TaskListView
import com.marzec.todo.view.TextListItem

@Composable
fun TaskDetailsScreen(
    store: TaskDetailsStore,
    actionBarProvider: ActionBarProvider
) {
    val state: State<TaskDetailsState> by store.collectState {
        store.loadDetails()
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide("Task details") {
                val subTasksCount = state.data?.task?.subTasks?.size ?: 0
                val selectedCount = state.data?.selected?.count() ?: 0
                val selectionModeEnabled =
                    state is State.Data<TaskDetailsState> && selectedCount > 0

                state.ifDataAvailable {
                    SearchView(search, store)
                }
                IconButton({
                    store.edit()
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }
                if (!selectionModeEnabled) {
                    IconButton({
                        store.showRemoveTaskDialog()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove"
                        )
                    }
                }
                if ((state.data?.task?.description?.lines()?.size ?: 0) > 1) {
                    IconButton({
                        store.explodeIntoTasks(
                            state.data?.task?.description?.lines().orEmpty()
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Explode"
                        )
                    }
                }
                if (selectionModeEnabled) {
                    IconButton({
                        store.showRemoveSelectedSubTasksDialog()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove"
                        )
                    }
                    IconButton({
                        store.unpinSubtasks()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Unpin all"
                        )
                    }
                }
                if (state.data?.task?.subTasks?.any { !it.isToDo } == true) {
                    IconButton({
                        store.removeDoneTasks()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remove"
                        )
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Remove"
                        )
                    }
                }
                if (state is State.Data<TaskDetailsState> && subTasksCount > 0) {
                    val selected = subTasksCount == selectedCount
                    Checkbox(
                        checked = selected,
                        onCheckedChange = {
                            store.onAllSelectClicked()
                        }
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    store.addSubTask()
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add subtask")
            }
        }
    ) {
        when (val state = state) {
            is State.Data -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Row(
                        modifier = Modifier
                            .padding(all = 16.dp)
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            SelectionContainer {
                                Text(text = state.data.task.description, fontSize = 16.sp)
                            }
                        }
                        val urls = state.data.task.description.urls()
                        if (urls.isNotEmpty()) {
                            Spacer(Modifier.size(16.dp))
                            IconButton({
                                store.openUrls(urls)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Open url"
                                )
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                        IconButton({
                            store.copyDescription()
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Copy")
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                    TaskListView(
                        tasks = state.data.task.subTasks,
                        search = state.data.search.value,
                        selected = state.data.selected,
                        showButtonsInColumns = true,
                        onClickListener = {
                            store.goToSubtaskDetails(it)
                        },
                        onOpenUrlClick = {
                            store.openUrl(it)
                        },
                        onMoveToTopClick = {
                            store.moveToTop(it)
                        },
                        onMoveToBottomClick = {
                            store.moveToBottom(it)
                        },
                        onRemoveButtonClick = {
                            store.showRemoveSubTaskDialog(it)
                        },
                        onPinButtonClick = {
                            store.unpinSubtask(it)
                        },
                        onCheckClick = {
                            store.markAsChecked(it)
                        },
                        onUncheckClick = {
                            store.markAsToDo(it)
                        },
                        onSelectedChange = {
                            store.onSelectedChange(it)
                        }
                    )
                }
                ShowDialog(store, state.data.dialog)
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
fun ShowDialog(store: TaskDetailsStore, dialog: DialogState) {
    when (dialog) {
        is DialogState.RemoveDialog -> {
            DialogBox(
                state = Dialog.TwoOptionsDialog(
                    title = "Remove task",
                    message = if (dialog.idsToRemove.size > 1) {
                        "Do you really want to remove this tasks?"
                    } else {
                        "Do you really want to remove this task?"
                    },
                    confirmButton = "Yes",
                    dismissButton = "No",
                    onDismiss = { store.closeDialog() },
                    onConfirm = {
                        store.removeTask(dialog.idsToRemove)
                    }
                )
            )

        }
        is DialogState.RemoveDialogWithCheckBox -> {
            DialogBox(
                state = Dialog.TwoOptionsDialogWithCheckbox(
                    twoOptionsDialog = Dialog.TwoOptionsDialog(
                        title = "Remove task",
                        message = if (dialog.idsToRemove.size > 1) {
                            "Do you really want to remove this tasks?"
                        } else {
                            "Do you really want to remove this task?"
                        },
                        confirmButton = "Yes",
                        dismissButton = "No",
                        onDismiss = { store.closeDialog() },
                        onConfirm = {
                            store.removeTask(dialog.idsToRemove)
                        }
                    ),
                    checked = dialog.checked,
                    checkBoxLabel = "Remove with all sub-tasks",
                    onCheckedChange = {
                        store.onRemoveWithSubTasksChange()
                    }
                )
            )
        }
        is DialogState.SelectOptionsDialog -> {
            DialogBox(
                state = Dialog.SelectOptionsDialog(
                    items = dialog.items.filterIsInstance<String>()
                        .mapIndexed { index, url ->
                            TextListItem(
                                id = index.toString(),
                                description = url,
                                name = EMPTY_STRING
                            )
                        },
                    onDismiss = { store.closeDialog() },
                    onItemClicked = { id ->
                        store.openUrl(dialog.items[id.toInt()] as String)
                    }
                )
            )
        }
        else -> Unit
    }
}
