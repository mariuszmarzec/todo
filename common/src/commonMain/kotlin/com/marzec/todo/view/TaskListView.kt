package com.marzec.todo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.marzec.todo.extensions.urlToOpen
import com.marzec.todo.model.Task

private data class TaskListItem(
    val id: Int,
    val item: TextListItem,
    val isToDo: Boolean,
    val urlToOpen: String?
)

@Composable
fun TaskListView(
    tasks: List<Task>,
    showButtonsInColumns: Boolean,
    onClickListener: (Int) -> Unit,
    onOpenUrlClick: ((String) -> Unit)? = null,
    onMoveToTopClick: ((Int) -> Unit)? = null,
    onMoveToBottomClick: ((Int) -> Unit)? = null,
    onRemoveButtonClick: ((Int) -> Unit)? = null,
    onUnpinButtonClick: ((Int) -> Unit)? = null,
    onCheckClick: ((Int) -> Unit)? = null,
    onUncheckClick: ((Int) -> Unit)? = null
) {
    LazyColumn {
        items(
            items = tasks.map {
                TaskListItem(
                    id = it.id,
                    item = TextListItem(
                        id = it.id.toString(),
                        name = it.description.lines().first(),
                        description = it.subTasks.firstOrNull()?.description?.lines()
                            ?.first() ?: ""
                    ),
                    urlToOpen = it.urlToOpen(),
                    isToDo = it.isToDo
                )
            },
        ) { listItem ->
            val id = listItem.id
            key(id) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextListItemView(
                        listItem.item,
                        backgroundColor = if (listItem.isToDo) Color.White else Color.LightGray,
                        onClickListener = {
                            onClickListener(it.id.toInt())
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
                                    onRemoveButtonClick,
                                    onUnpinButtonClick
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
                            ManageButtons(id, onRemoveButtonClick, onUnpinButtonClick)
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
    onRemoveButtonClick: ((Int) -> Unit)?,
    onUnpinButtonClick: ((Int) -> Unit)?
) {
    onRemoveButtonClick?.let { onClick ->
        IconButton({ onClick(id) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove"
            )
        }
    }
    onUnpinButtonClick?.let { onClick ->
        IconButton({ onClick(id) }) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Unpin"
            )
        }
    }
}
