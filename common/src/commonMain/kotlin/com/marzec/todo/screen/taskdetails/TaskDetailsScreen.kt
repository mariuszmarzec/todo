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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.mvi.State
import com.marzec.extensions.EMPTY_STRING
import com.marzec.mvi.collectState
import com.marzec.extensions.urls
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.view.ActionBarProvider
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
            actionBarProvider.provide {
                state.ifDataAvailable {
                    SearchView(search, store)
                    IconButton({
                        store.edit()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    IconButton({
                        store.showRemoveTaskDialog()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove"
                        )
                    }
                    if (task.description.lines().size > 1) {
                        IconButton({
                            store.explodeIntoTasks(task.description.lines())
                        }) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Explode"
                            )
                        }
                    }

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

                    val subTasksCount = state.data.task.subTasks.size
                    val selectedCount = state.data.selected.count()
                    val selectionModeEnabled = selectedCount > 0
                    if (subTasksCount > 0) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectionModeEnabled) {
                                IconButton({
                                    store.markSelectedAsTodo()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Mark selected as to do"
                                    )
                                }
                                IconButton({
                                    store.markSelectedAsDone()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Mark selected as done"
                                    )
                                }

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
                            if (state.data.task.subTasks.any { !it.isToDo }) {
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
                            Checkbox(
                                checked = subTasksCount == selectedCount,
                                onCheckedChange = {
                                    store.onAllSelectClicked()
                                }
                            )
                        }
                    }
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
                            IconButton({
                                store.openUrls(urls)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Open url"
                                )
                            }
                        }
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
                            store.markAsDone(it)
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
