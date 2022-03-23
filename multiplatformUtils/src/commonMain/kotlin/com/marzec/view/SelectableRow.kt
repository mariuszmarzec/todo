package com.marzec.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SelectableRow(
    backgroundColor: Color,
    selectable: Boolean,
    selected: Boolean,
    onSelectedChange: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxWidth().let {
            if (selectable) {
                it.clickable { onSelectedChange() }
            } else {
                it
            }
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectable) {
            Checkbox(
                checked = selected,
                modifier = Modifier.padding(16.dp, 16.dp, 0.dp, 16.dp),
                onCheckedChange = { onSelectedChange() }
            )
        }
        content()
    }
}
