package com.marzec.todo.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marzec.extensions.applyIf
import com.marzec.todo.extensions.descriptionWithProgress
import com.marzec.todo.extensions.filterWithSearch
import com.marzec.extensions.ifFalse
import com.marzec.modifier.dragAndDrop
import com.marzec.todo.extensions.subDescription
import com.marzec.todo.extensions.urlToOpen
import com.marzec.todo.model.Task
import com.marzec.view.SelectableRow
import com.marzec.view.TextListItem
import com.marzec.view.TextListItemView

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
    reorderMode: Boolean = false,
    showButtonsInColumns: Boolean,
    scrollState: LazyListState = rememberLazyListState(),
    onClickListener: (Int) -> Unit,
    onOpenUrlClick: ((String) -> Unit)? = null,
    onMoveToTopClick: ((Int) -> Unit)? = null,
    onMoveToBottomClick: ((Int) -> Unit)? = null,
    onRemoveButtonClick: ((Int) -> Unit)? = null,
    onPinButtonClick: ((Int) -> Unit)? = null,
    onCheckClick: ((Int) -> Unit)? = null,
    onUncheckClick: ((Int) -> Unit)? = null,
    onSelectedChange: ((Int) -> Unit)? = null,
    onDragAndDrop: ((draggedIndex: Int, targetIndex: Int) -> Unit)? = null
) {

    val selectionModeEnabled = selected.isNotEmpty()

    val dragEnteredIndex: MutableIntState = remember { mutableIntStateOf(-1) }

    val selectable = selectionModeEnabled && !reorderMode
    LazyColumn(state = scrollState) {
        itemsIndexed(
            items = tasks
                .filterWithSearch(search)
                .map {
                    TaskListItem(
                        id = it.id,
                        item = TextListItem(
                            id = it.id.toString(),
                            name = it.descriptionWithProgress,
                            description = it.subDescription
                        ),
                        urlToOpen = it.urlToOpen(),
                        isToDo = it.isToDo,
                        pinned = it.parentTaskId != null
                    )
                },
        ) { index, listItem ->
            val id = listItem.id
            val selected = id in selected
            key(id) {
                Row(modifier = Modifier
                    .height(IntrinsicSize.Max)
                    .applyIf({ dragEnteredIndex.value == index }) {
                        this.background(Color.LightGray)
                    }
                    .dragAndDrop(
                    index = index,
                    dragEnteredIndex = dragEnteredIndex,
                    onDrop = { draggedIndex: Int, targetIndex: Int ->
                        onDragAndDrop?.invoke(draggedIndex, targetIndex)
                    }
                )) {
                    if (reorderMode) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(56.dp)
                                .background(Color.Gray)
                        )
                    }
                    SelectableRow(
                        backgroundColor = when {
                            selected -> Color.Gray
                            !listItem.isToDo -> Color.LightGray
                            else -> Color.Transparent
                        },
                        selectable = selectable,
                        selected = selected,
                        onSelectedChange = { onSelectedChange?.invoke(id) }
                    ) {
                        TextListItemView(
                            state = listItem.item,
                            backgroundColor = Color.Transparent,
                            onLongClickListener = selectable.ifFalse {
                                onSelectedChange?.let { { it(id) } }
                            },
                            onClickListener = selectable.ifFalse {
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
