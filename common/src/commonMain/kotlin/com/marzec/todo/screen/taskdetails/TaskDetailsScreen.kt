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
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.emptyString
import com.marzec.todo.extensions.urls
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.todo.view.ActionBarProvider
import com.marzec.todo.view.Dialog
import com.marzec.todo.view.DialogBox
import com.marzec.todo.view.DialogState
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import com.marzec.todo.view.TwoOptionsDialogWithCheckbox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TaskDetailsScreen(
    listId: Int,
    taskId: Int,
    store: TaskDetailsStore,
    actionBarProvider: ActionBarProvider
) {

    val scope = rememberCoroutineScope()

    val state: TaskDetailsState by store.state.collectAsState()

    LaunchedEffect(listId, taskId) {
        store.init(scope)
        scope.launch { store.loadDetails() }
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide("Task details") {
                IconButton({
                    scope.launch { store.edit() }
                }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }
                Spacer(Modifier.size(16.dp))
                IconButton({
                    scope.launch { store.showRemoveTaskDialog() }
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove"
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch { store.addSubTask() }
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add subtask")
            }
        }
    ) {
        when (val state = state) {
            is TaskDetailsState.Data -> {
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
                                Text(text = state.task.description, fontSize = 16.sp)
                            }
                        }
                        val urls = state.task.description.urls()
                        if (urls.isNotEmpty()) {
                            Spacer(Modifier.size(16.dp))
                            IconButton({
                                scope.launch { store.openUrls(urls) }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Open url"
                                )
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                        IconButton({
                            scope.launch { store.copyDescription() }
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Copy")
                        }
                    }
                    Spacer(Modifier.size(16.dp))
                    LazyColumn {
                        items(
                            items = state.task.subTasks.map {
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
                                        IconButton({
                                            scope.launch { store.moveToTop(it.id) }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move to top"
                                            )
                                        }
                                        IconButton({
                                            scope.launch { store.moveToBottom(it.id) }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move to bottom"
                                            )
                                        }
                                        IconButton({
                                            scope.launch { store.unpinSubtask(it.id) }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Unpin"
                                            )
                                        }
                                        IconButton({
                                            scope.launch { store.showRemoveSubTaskDialog(it.id) }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                when (val dialog = state.dialog) {
                    is DialogState.RemoveDialog -> {
                        DialogBox(
                            state = Dialog.TwoOptionsDialog(
                                title = "Remove task",
                                message = "Do you really want to remove this task?",
                                confirmButton = "Yes",
                                dismissButton = "No",
                                onDismiss = { scope.launch { store.hideDialog() } },
                                onConfirm = {
                                    scope.launch { store.removeTask(dialog.idToRemove) }
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
                                    onDismiss = { scope.launch { store.hideDialog() } },
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
                    is DialogState.SelectOptionsDialog -> {
                        DialogBox(
                            state = Dialog.SelectOptionsDialog(
                                items = dialog.items.filterIsInstance<String>()
                                    .mapIndexed { index, url ->
                                        TextListItem(
                                            id = index.toString(),
                                            description = url,
                                            name = emptyString()
                                        )
                                    },
                                onDismiss = { scope.launch { store.hideDialog() } },
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
            is TaskDetailsState.Loading -> {
                Text(text = "Loading")
            }
            is TaskDetailsState.Error -> {
                Text(text = state.message)
            }
        }
    }
}
