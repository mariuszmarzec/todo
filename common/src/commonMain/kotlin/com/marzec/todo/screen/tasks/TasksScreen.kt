package com.marzec.todo.screen.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.marzec.delegate.DialogState
import com.marzec.delegate.rememberScrollState
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.todo.delegates.reorder.ReorderMode
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.todo.screen.tasks.model.TasksStore.Companion.DIALOG_ID_REMOVE_MULTIPLE_TASKS
import com.marzec.todo.view.ManageTaskSelectionBar
import com.marzec.todo.view.TaskListView
import com.marzec.view.ActionBarProvider
import com.marzec.view.Dialog
import com.marzec.view.DialogBox
import com.marzec.view.ScreenWithLoading
import com.marzec.view.SearchView

@Composable
fun TasksScreen(store: TasksStore, actionBarProvider: ActionBarProvider) {

    val state: State<TasksScreenState> by store.collectState {
        store.loadList()
    }

    val listState = rememberScrollState(store)

    Scaffold(
        topBar = {
            val count = state.data?.tasks?.count()?.let { " ($it)" }.orEmpty()
            actionBarProvider.provide(title = "Tasks$count") {
                if (state.data?.reorderMode is ReorderMode.Disabled) {
                    state.ifDataAvailable {
                        SearchView(search, store)
                    }
                    IconButton({
                        store.onScheduledClick()
                    }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Scheduled")
                    }
                }

                if (state.data?.reorderMode is ReorderMode.Enabled) {
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
                        Icon(imageVector = Icons.Default.Done, contentDescription = "Save")
                    }
                } else {
                    IconButton({
                        store.enableReorderMode()
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Reorder")
                    }
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
            if (state.data?.reorderMode !is ReorderMode.Enabled) {
                FloatingActionButton(
                    onClick = {
                        store.addNewTask()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add new")
                }
            }
        }) {
        ScreenWithLoading(
            state = state,
            onReloadClick = { store.loadList() }
        ) {
            TaskScreenData(it, store, listState)
        }
    }
}

@Composable
private fun TaskScreenData(
    state: State.Data<TasksScreenState>,
    store: TasksStore,
    scrollState: LazyListState = rememberLazyListState()
) {
    Column {
        ManageTaskSelectionBar(
            tasks = state.data.tasks,
            selected = state.data.selected,
            shouldShow = state.data.selected.isNotEmpty(),
            onRemoveClick = {
                store.showRemoveSelectedSubTasksDialog()
            },
            onAllSelectClicked = {
                store.onAllSelectClicked()
            }
        )

        TaskListView(
            tasks = (state.data.reorderMode as? ReorderMode.Enabled)?.items ?: state.data.tasks,
            search = state.data.search.value,
            selected = state.data.selected,
            reorderMode = state.data.reorderMode is ReorderMode.Enabled,
            showButtonsInColumns = false,
            scrollState = scrollState,
            onClickListener = {
                store.onListItemClicked(it)
            },
            onSelectedChange = {
                store.onSelectedChange(it)
            },
            onOpenUrlClick = { store.openUrl(it) },
            onMoveToTopClick = { store.moveToTop(it) },
            onMoveToBottomClick = {
                store.moveToBottom(it)
            },
            onRemoveButtonClick = {
                store.onRemoveButtonClick(it)
            },
            onDragAndDrop = { draggedIndex: Int, targetIndex: Int ->
                store.onDragged(draggedIndex, targetIndex)
            }
        )
    }

    val dialog = state.data.dialog
    when (dialog) {
        is DialogState.RemoveDialogWithCheckBox -> {
            RemoveTaskWithCheckBox(store, dialog)
        }

        is DialogState.RemoveDialog -> {
            RemoveTaskDialog(store, dialog)
        }

        else -> Unit
    }
}

@Composable
private fun RemoveTaskWithCheckBox(
    store: TasksStore,
    dialog: DialogState.RemoveDialogWithCheckBox<Int>
) {
    when (dialog.id) {
        DIALOG_ID_REMOVE_MULTIPLE_TASKS -> {
            DialogBox(
                state = Dialog.TwoOptionsDialogWithCheckbox(
                    twoOptionsDialog = Dialog.TwoOptionsDialog(
                        title = "Remove selected tasks",
                        message = "Do you really want to remove selected tasks?",
                        confirmButton = "Yes",
                        dismissButton = "No",
                        onDismiss = { store.closeDialog() },
                        onConfirm = { store.removeTask(dialog.idsToRemove) }
                    ),
                    checked = dialog.checked,
                    checkBoxLabel = "Remove with all sub-tasks",
                    onCheckedChange = { store.onRemoveWithSubTasksChange() }
                )
            )
        }

        else -> {
            DialogBox(
                state = Dialog.TwoOptionsDialogWithCheckbox(
                    twoOptionsDialog = Dialog.TwoOptionsDialog(
                        title = "Remove task",
                        message = "Do you really want to remove this task?",
                        confirmButton = "Yes",
                        dismissButton = "No",
                        onDismiss = { store.closeDialog() },
                        onConfirm = { store.removeTask(dialog.idsToRemove) }
                    ),
                    checked = dialog.checked,
                    checkBoxLabel = "Remove with all sub-tasks",
                    onCheckedChange = { store.onRemoveWithSubTasksChange() }
                )
            )
        }
    }
}

@Composable
private fun RemoveTaskDialog(
    store: TasksStore,
    dialog: DialogState.RemoveDialog<Int>
) {
    DialogBox(
        state = Dialog.TwoOptionsDialog(
            title = "Remove task",
            message = "Do you really want to remove this task?",
            confirmButton = "Yes",
            dismissButton = "No",
            onDismiss = { store.closeDialog() },
            onConfirm = { store.removeTask(dialog.idsToRemove) }
        )
    )
}
