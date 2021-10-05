package com.marzec.todo.screen.tasks

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.marzec.mvi.State
import com.marzec.todo.extensions.emptyString
import com.marzec.todo.extensions.urlToOpen
import com.marzec.todo.screen.tasks.model.TasksScreenState
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.todo.view.ActionBarProvider
import com.marzec.todo.view.Dialog
import com.marzec.todo.view.DialogBox
import com.marzec.todo.view.DialogState
import com.marzec.todo.view.TextListItem
import com.marzec.todo.view.TextListItemView
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(store: TasksStore, actionBarProvider: ActionBarProvider) {
    val scope = rememberCoroutineScope()

    val state: State<TasksScreenState> by store.state.collectAsState()

    LaunchedEffect(Unit) {
        store.init(this) {
            store.loadList()
        }
    }

    Scaffold(
        topBar = {
            actionBarProvider.provide(title = "Tasks") {
                when (val state = state) {
                    is State.Data<TasksScreenState> -> {
                        Row(
                            Modifier.wrapContentWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            val focusManager = LocalFocusManager.current
                            val focusRequester = remember { FocusRequester() }
                            val searchInUse =
                                state.data.searchFocused || state.data.search.isNotEmpty()
                            BasicTextField(
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { scope.launch {
                                        store.onSearchFocusChanged(it.isFocused) }
                                    }
                                    .widthIn(min = 200.dp, max = 300.dp)
                                    .padding(0.dp),
                                singleLine = true,
                                value = if (searchInUse) {
                                    state.data.search
                                } else "Search",
                                onValueChange = {
                                    scope.launch { store.onSearchQueryChanged(it) }
                                }
                            )
                            LaunchedEffect(key1 = state.data.searchFocused) {
                                if (state.data.searchFocused) {
                                    focusRequester.requestFocus()
                                } else {
                                    focusManager.clearFocus()

                                }
                            }
                            if (searchInUse) {
                                IconButton({
                                    scope.launch { store.clearSearch() }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            } else {
                                IconButton({
                                    scope.launch { store.activateSearch() }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Activate search"
                                    )
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch { store.addNewTask() }
                }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add new")
            }
        }) {
        when (val state = state) {
            is State.Data -> {
                LazyColumn {
                    val searchQuery = state.data.search.trim().split(" ")
                    items(
                        items = state.data.tasks.filter { task ->
                            searchQuery == listOf(emptyString()) || searchQuery.all {
                                task.description.contains(
                                    it,
                                    ignoreCase = true
                                )
                            }
                        }.map {
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
                                TextListItemView(state = it, onClickListener = {
                                    scope.launch {
                                        store.onListItemClicked(it.id)
                                    }
                                }) {
                                    // TODO REMOVE THIS LOGIC
                                    if (state.data.tasks.firstOrNull { task -> task.id == it.id.toInt() }
                                            ?.urlToOpen() != null
                                    ) {
                                        IconButton({
                                            scope.launch { store.openUrl(it.id) }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.ExitToApp,
                                                contentDescription = "Open url"
                                            )
                                        }
                                    }
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
                                        scope.launch { store.showRemoveDialog(it.id) }
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
                    else -> { }
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