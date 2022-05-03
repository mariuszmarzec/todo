package com.marzec.screen.pickitemscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.view.ActionBarProvider
import com.marzec.view.ScreenWithLoading
import com.marzec.view.SelectableRow
import androidx.compose.ui.Modifier

@Composable
fun <ITEM : Any> PickItemScreen(
    options: PickItemOptions<ITEM>,
    store: PickItemDataStore<ITEM>,
    actionBarProvider: ActionBarProvider
) {

    val state: State<PickItemData<ITEM>> by store.collectState {
        store.load()
    }

    Column(
        modifier = Modifier.background(Color.White)
    ) {
        actionBarProvider.provide {
            state.ifDataAvailable {
                if (options.multipleChoice) {
                    Checkbox(
                        checked = selected.size == allIds().size,
                        onCheckedChange = {
                            store.onAllSelectClicked()
                        }
                    )
                    IconButton({
                        store.onConfirmClick()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm choice"
                        )
                    }
                }
            }
        }

        ScreenWithLoading(
            state,
            { store.load() }
        ) { state ->
            Scaffold(
                floatingActionButton = {
                    if (options.onAddNavigationAction != null) {
                        FloatingActionButton(
                            onClick = {
                                store.onAddButtonClick()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add button")
                        }
                    }
                }
            ) {
                LazyColumn {
                    items(items = state.data.items) { item ->
                        val id = options.mapItemToId(item)
                        key(id) {
                            SelectableRow(
                                backgroundColor = Color.White,
                                selectable = options.multipleChoice,
                                selected = id in state.data.selected,
                                onSelectedChange = { store.onSelectedChange(id) },
                            ) {
                                options.itemRow(item) {
                                    store.onItemClick(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
