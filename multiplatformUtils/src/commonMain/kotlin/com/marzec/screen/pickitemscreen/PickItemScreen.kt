package com.marzec.screen.pickitemscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.marzec.mvi.State
import com.marzec.mvi.collectState
import com.marzec.view.ActionBarProvider
import com.marzec.view.ScreenWithLoading

@Composable
fun <ITEM> PickItemScreen(
    options: PickItemOptions<ITEM>,
    store: PickItemDataStore<ITEM>,
    actionBarProvider: ActionBarProvider
) {

    val state: State<PickItemData<ITEM>> by store.collectState {
        store.load()
    }

    Column {
        actionBarProvider.provide {
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
                        key(options.mapItemToId(item)) {
                            options.itemRow(item) {
                                store.onItemClick(options.mapItemToId(it))
                            }
                        }
                    }
                }
            }
        }
    }
}
