package com.marzec.view

@Composable
private fun AddResourceView(
    showEmptyState: Boolean,
    onClick: () -> Unit,
    onRemoveAllButtonClick: () -> Unit,
    clearButtonContentDescription: String,
    label: String
) {
    if (!showEmptyState) {
        Row(
            modifier = Modifier.clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton({ onRemoveAllButtonClick() }) {
                Icon(Icons.Default.Clear, clearButtonContentDescription)
            }
            Text(
                text = label
            )
        }
    } else {
        Button(onClick = {
            onClick()
        }) {
            Text(label)
        }
    }
}
