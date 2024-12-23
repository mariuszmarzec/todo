package com.marzec.todo.screen.taskdetails

import androidx.compose.foundation.background
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
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.mvi.State
import com.marzec.extensions.EMPTY_STRING
import com.marzec.mvi.collectState
import com.marzec.extensions.urls
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.view.ActionBarProvider
import com.marzec.view.Dialog
import com.marzec.view.DialogBox
import com.marzec.delegate.DialogState
import com.marzec.todo.delegates.reorder.ReorderMode
import com.marzec.todo.view.ManageTaskSelectionBar
import com.marzec.todo.view.ShowCheck
import com.marzec.view.SearchView
import com.marzec.todo.view.TaskListView
import com.marzec.view.TextListItem
import com.marzec.view.rememberForeverListState

@Composable
fun TaskDetailsScreen(
    store: TaskDetailsStore,
    actionBarProvider: ActionBarProvider
) {
    val state: State<TaskDetailsState> by store.collectState {
        store.loadDetails()
    }

    val listState = rememberForeverListState(store.identifier)

    val topBarColor = if (state.data?.task?.isToDo == false) {
        Color.LightGray
    } else {
        Color.Transparent
    }
    Scaffold(
        topBar = {
            actionBarProvider.provide(
                backgroundColor = topBarColor
            ) {
                state.ifDataAvailable {
                    if (reorderMode is ReorderMode.Disabled) {
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

                    val task = state.data.task
                    val subTasksCount = task.subTasks.size
                    val doneSubtasksCount = task.subTasks.count { !it.isToDo }

                    if (state.data.task.subTasks.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().background(topBarColor),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (state.data.search.value.isEmpty() && !state.data.search.focused) {
                                if (state.data.reorderMode is ReorderMode.Enabled) {
                                    IconButton({
                                        store.disableReorderMode()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close reorder"
                                        )
                                    }
                                    IconButton({
                                        store.saveReorder()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Done,
                                            contentDescription = "Save"
                                        )
                                    }
                                } else {
                                    IconButton({
                                        store.enableReorderMode()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Reorder"
                                        )
                                    }
                                }
                            }
                            ManageTaskSelectionBar(
                                tasks = (state.data.reorderMode as? ReorderMode.Enabled)?.items
                                    ?: task.subTasks,
                                selected = state.data.selected,
                                shouldShow = task.subTasks.isNotEmpty(),
                                onMarkSelectedAsTodoClick = {
                                    store.markSelectedAsTodo()
                                },
                                onMarkSelectedAsDoneClick = {
                                    store.markSelectedAsDone()
                                },
                                onRemoveClick = {
                                    store.showRemoveSelectedSubTasksDialog()
                                },
                                onRemoveDoneTasksClick = {
                                    store.removeDoneTasks()
                                },
                                onAllSelectClicked = {
                                    store.onAllSelectClicked()
                                },
                                onUnpinSubtasksClick = {
                                    store.unpinSubtasks()
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
                                Text(text = task.description, fontSize = 16.sp)
                            }
                        }
                        val urls = task.description.urls()
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
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Copy description"
                            )
                        }
                        IconButton({
                            store.copyTask()
                        }) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Copy task"
                            )
                        }
                        ShowCheck(
                            id = task.id,
                            isToDo = task.isToDo,
                            onCheckClick = {
                                store.markAsDone(it)
                            },
                            onUncheckClick = {
                                store.markAsToDo(it)
                            }
                        )
                    }
                    if (subTasksCount > 0) {
                        val subTasksCountRow = if (doneSubtasksCount > 0) {
                            "$doneSubtasksCount/$subTasksCount"
                        } else {
                            subTasksCount
                        }
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .align(Alignment.End),
                            text = "Subtasks: $subTasksCountRow"
                        )
                    }
                    Spacer(Modifier.size(16.dp))
                    TaskListView(
                        tasks = (state.data.reorderMode as? ReorderMode.Enabled)?.items
                            ?: task.subTasks,
                        search = state.data.search.value,
                        selected = state.data.selected,
                        reorderMode = state.data.reorderMode is ReorderMode.Enabled,
                        scrollState = listState,
                        showButtonsInColumns = state.data.reorderMode !is ReorderMode.Enabled,
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
                        },
                        onDragAndDrop = { draggedIndex: Int, targetIndex: Int ->
                            store.onDragged(draggedIndex, targetIndex)
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
fun ShowDialog(store: TaskDetailsStore, dialog: DialogState<Int>) {
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
