package com.marzec.todo.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CheckableRow(
    checkable: Boolean,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().let {
            if (checkable) {
                it.clickable { onCheckedChange() }
            } else {
                it
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (checkable) {
            Checkbox(
                checked = checked,
                modifier = Modifier.padding(16.dp, 16.dp, 0.dp, 16.dp),
                onCheckedChange = { onCheckedChange() }
            )
        }
        content()
    }
}
