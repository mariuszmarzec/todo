package com.marzec.todo.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.marzec.todo.model.Task

@Composable
fun ManageTaskSelectionBar(
    tasks: List<Task>,
    selected: Set<Int>,
    shouldShow: Boolean,
    onMarkSelectedAsTodoClick: (() -> Unit)? = null,
    onMarkSelectedAsDoneClick: (() -> Unit)? = null,
    onRemoveClick: () -> Unit,
    onRemoveDoneTasksClick: (() -> Unit)? = null,
    onAllSelectClicked: () -> Unit,
    onUnpinSubtasksClick: (() -> Unit)? = null
) {
    val subTasksCount = tasks.size
    val selectedCount = selected.count()
    val selectionModeEnabled = selectedCount > 0

    if (shouldShow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCount > 0) {
                Text("($selectedCount/$subTasksCount)")
            }
            if (selectionModeEnabled) {
                onMarkSelectedAsTodoClick?.let {
                    IconButton({
                        onMarkSelectedAsTodoClick()
                    }) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Mark selected as to do"
                        )
                    }
                }
                onMarkSelectedAsDoneClick?.let {
                    IconButton({
                        onMarkSelectedAsDoneClick()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark selected as done"
                        )
                    }
                }

                IconButton({
                    onRemoveClick()
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove"
                    )
                }
                onUnpinSubtasksClick?.let {
                    IconButton({
                        onUnpinSubtasksClick()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Unpin all"
                        )
                    }
                }
            }
            if (tasks.any { !it.isToDo } && onRemoveDoneTasksClick != null) {
                IconButton({
                    onRemoveDoneTasksClick()
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
                    onAllSelectClicked()
                }
            )
        }
    }
}
