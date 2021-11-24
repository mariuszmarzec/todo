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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.mvi.State
import com.marzec.todo.extensions.EMPTY_STRING
import com.marzec.todo.extensions.urlToOpen
import com.marzec.todo.extensions.urls
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.view.ActionBarProvider
import com.marzec.todo.view.Dialog
import com.marzec.todo.view.DialogBox
import com.marzec.todo.view.DialogState
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import kotlinx.coroutines.launch

@Composable
fun TaskDetailsScreen(
    listId: Int,
    taskId: Int,
    store: TaskDetailsStore,
    actionBarProvider: ActionBarProvider
) {

    val scope = rememberCoroutineScope()

    val state: State<TaskDetailsState> by store.state.collectAsState()

    LaunchedEffect(listId, taskId) {
        store.init(scope) { store.loadDetails() }
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide("Task details") {
                IconButton({
                    store.edit()
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }
                Spacer(Modifier.size(16.dp))
                IconButton({
                    store.showRemoveTaskDialog()
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove"
                    )
                }
                if ((state.data?.task?.description?.lines()?.size ?: 0) > 1) {
                    IconButton({
                        store.explodeIntoTasks(
                            state?.data?.task?.description?.lines().orEmpty()
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Explode"
                        )
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
                    LazyColumn {
                        items(
                            items = state.data.task.subTasks.map {
                                TextListItem(
                                    id = it.id.toString(),
                                    name = it.description.lines().first(),
                                    description = it.subTasks.firstOrNull()?.description?.lines()
                                        ?.first() ?: ""
                                )
                            },
                        ) {
                            key(it.id) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextListItemView(
                                        state = it,
                                        onClickListener = {
                                            scope.launch {
                                                store.goToSubtaskDetails(it.id)
                                            }
                                        }
                                    ) {
                                        // TODO REMOVE THIS LOGIC
                                        val urlToOpen =
                                            state.data.task.subTasks.firstOrNull { task -> task.id == it.id.toInt() }
                                                ?.urlToOpen()
                                        if (urlToOpen != null) {
                                            IconButton({
                                                store.openUrl(urlToOpen)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.ExitToApp,
                                                    contentDescription = "Open url"
                                                )
                                            }
                                        }
                                        Column {
                                            IconButton({
                                                store.moveToTop(it.id)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Move to top"
                                                )
                                            }
                                            IconButton({
                                                store.moveToBottom(it.id)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Move to bottom"
                                                )
                                            }
                                        }
                                        Column {
                                            IconButton({
                                                store.showRemoveSubTaskDialog(it.id)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove"
                                                )
                                            }
                                            IconButton({
                                                store.unpinSubtask(it.id)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Unpin"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                when (val dialog = state.data.dialog) {
                    is DialogState.RemoveDialog -> {
                        DialogBox(
                            state = Dialog.TwoOptionsDialog(
                                title = "Remove task",
                                message = "Do you really want to remove this task?",
                                confirmButton = "Yes",
                                dismissButton = "No",
                                onDismiss = { store.closeDialog() },
                                onConfirm = {
                                    store.removeTask(dialog.idToRemove)
                                }
                            )
                        )

                    }
                    is DialogState.RemoveDialogWithCheckBox -> {
                        DialogBox(
                            state = Dialog.TwoOptionsDialogWithCheckbox(
                                twoOptionsDialog = Dialog.TwoOptionsDialog(
                                    title = "Remove task",
                                    message = "Do you really want to remove this task?",
                                    confirmButton = "Yes",
                                    dismissButton = "No",
                                    onDismiss = { store.closeDialog() },
                                    onConfirm = {
                                        store.removeTask(dialog.idToRemove)
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
                                    scope.launch {
                                        store.openUrl(dialog.items[id.toInt()] as String)
                                    }
                                }
                            )
                        )
                    }
                    else -> Unit
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
