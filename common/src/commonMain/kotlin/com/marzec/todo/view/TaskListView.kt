package com.marzec.todo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import com.marzec.todo.extensions.descriptionWithProgress
import com.marzec.todo.extensions.filterWithSearch
import com.marzec.todo.extensions.ifFalse
import com.marzec.todo.extensions.urlToOpen
import com.marzec.todo.model.Task

private data class TaskListItem(
    val id: Int,
    val item: TextListItem,
    val pinned: Boolean,
    val isToDo: Boolean,
    val urlToOpen: String?
)

@Composable
fun TaskListView(
    tasks: List<Task>,
    search: String = "",
    selected: Set<Int>,
    showButtonsInColumns: Boolean,
    onClickListener: (Int) -> Unit,
    onOpenUrlClick: ((String) -> Unit)? = null,
    onMoveToTopClick: ((Int) -> Unit)? = null,
    onMoveToBottomClick: ((Int) -> Unit)? = null,
    onRemoveButtonClick: ((Int) -> Unit)? = null,
    onPinButtonClick: ((Int) -> Unit)? = null,
    onCheckClick: ((Int) -> Unit)? = null,
    onUncheckClick: ((Int) -> Unit)? = null,
    onSelectedChange: ((Int) -> Unit)? = null,
) {
    val selectionModeEnabled = selected.isNotEmpty()
    LazyColumn {
        items(
            items = tasks
            .filterWithSearch(search)
            .map {
                TaskListItem(
                    id = it.id,
                    item = TextListItem(
                        id = it.id.toString(),
                        name = it.descriptionWithProgress,
                        description = it.subTasks.firstOrNull()?.description?.lines()
                            ?.first() ?: ""
                    ),
                    urlToOpen = it.urlToOpen(),
                    isToDo = it.isToDo,
                    pinned = it.parentTaskId != null
                )
            },
        ) { listItem ->
            val id = listItem.id
            val selected = id in selected
            key(id) {
                SelectableRow(
                    backgroundColor = when {
                        selected -> Color.Gray
                        !listItem.isToDo -> Color.LightGray
                        else -> Color.White
                    },
                    selectable = selectionModeEnabled,
                    selected = selected,
                    onSelectedChange = { onSelectedChange?.invoke(id) }
                ) {
                    TextListItemView(
                        state = listItem.item,
                        backgroundColor = Color.Transparent,
                        onLongClickListener = selectionModeEnabled.ifFalse {
                            onSelectedChange?.let { { it(id) } }
                        },
                        onClickListener = selectionModeEnabled.ifFalse {
                            { onClickListener(it.id.toInt()) }
                        }
                    ) {
                        if (showButtonsInColumns) {
                            Column {
                                OpenUrl(listItem.urlToOpen, onOpenUrlClick)
                                ShowCheck(
                                    listItem.id,
                                    listItem.isToDo,
                                    onCheckClick,
                                    onUncheckClick
                                )
                            }
                            Column {
                                MoveButtons(id, onMoveToTopClick, onMoveToBottomClick)
                            }
                            Column {
                                ManageButtons(
                                    id,
                                    pinned = listItem.pinned,
                                    onRemoveButtonClick,
                                    onPinButtonClick
                                )
                            }
                        } else {
                            OpenUrl(listItem.urlToOpen, onOpenUrlClick)
                            ShowCheck(
                                listItem.id,
                                listItem.isToDo,
                                onCheckClick,
                                onUncheckClick
                            )
                            MoveButtons(id, onMoveToTopClick, onMoveToBottomClick)
                            ManageButtons(
                                id = id,
                                pinned = listItem.pinned,
                                onRemoveButtonClick = onRemoveButtonClick,
                                onPinButtonClick = onPinButtonClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenUrl(urlToOpen: String?, onOpenUrlClick: ((String) -> Unit)?) {
    if (urlToOpen != null) {
        onOpenUrlClick?.let { onOpenUrlClick ->
            IconButton({
                onOpenUrlClick(urlToOpen)
            }) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Open url"
                )
            }
        }
    }
}

@Composable
private fun ShowCheck(
    id: Int,
    isToDo: Boolean,
    onCheckClick: ((Int) -> Unit)?,
    onUncheckClick: ((Int) -> Unit)?
) {
    if (isToDo) {
        onCheckClick?.let { onCheckClick ->
            IconButton({
                onCheckClick(id)
            }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Mark as checked"
                )
            }
        }
    } else {
        onUncheckClick?.let { onUncheckClick ->
            IconButton({
                onUncheckClick(id)
            }) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Mark as unchecked"
                )
            }
        }
    }
}

@Composable
private fun MoveButtons(
    id: Int,
    onMoveToTopClick: ((Int) -> Unit)?,
    onMoveToBottomClick: ((Int) -> Unit)?
) {
    onMoveToTopClick?.let { onClick ->
        IconButton({ onClick(id) }) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Move to top"
            )
        }
    }
    onMoveToBottomClick?.let { onClick ->
        IconButton({ onClick(id) }) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Move to bottom"
            )
        }
    }
}

@Composable
private fun ManageButtons(
    id: Int,
    pinned: Boolean,
    onRemoveButtonClick: ((Int) -> Unit)?,
    onPinButtonClick: ((Int) -> Unit)?
) {
    onRemoveButtonClick?.let { onClick ->
        IconButton({ onClick(id) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove"
            )
        }
    }
    onPinButtonClick?.let { onClick ->
        IconButton({ onClick(id) }) {
            Icon(
                imageVector = if (pinned) Icons.Default.Clear else Icons.Default.Add,
                contentDescription = "Unpin"
            )
        }
    }
}
