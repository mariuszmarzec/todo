package com.marzec.screen.pickitemscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import com.marzec.delegate.rememberScrollState
import com.marzec.extensions.filterWithSearch
import com.marzec.view.SearchView

@Composable
fun <ITEM : Any> PickItemScreen(
    options: PickItemOptions<ITEM>,
    store: PickItemDataStore<ITEM>,
    actionBarProvider: ActionBarProvider
) {

    val state: State<PickItemData<ITEM>> by store.collectState {
        store.load()
    }
    val scrollState = rememberScrollState(store)

    Column(
        modifier = Modifier.background(Color.White)
    ) {
        actionBarProvider.provide {
            state.ifDataAvailable {
                if (options.stringsToCompare != null) {
                    SearchView(search, store)
                }
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
                val items: List<PickItem> = state.data.items.let {
                    val stringsToCompare = options.stringsToCompare
                    if (stringsToCompare != null) {
                        it.filterWithSearch(state.data.search.value, stringsToCompare)
                    } else {
                        it
                    }
                }.let { items ->
                    options.groupByHeader?.let { groupByHeader ->
                        items.groupBy(groupByHeader).flatMap { (header, items) ->
                            listOf(PickItem.Header(header)) + items.map { item -> PickItem.Item(item) }
                        }
                    } ?: items.map { item -> PickItem.Item(item) }
                }
                LazyColumn(state = scrollState) {
                    items(items = items) { pickItem ->
                        when (pickItem) {
                            is PickItem.Item<*> -> {
                                val item = pickItem.item as ITEM
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

                            is PickItem.Header -> {
                                Text(
                                    modifier = Modifier.background(Color.LightGray)
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    text = pickItem.title
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class PickItem {
    data class Item<ITEM : Any>(val item: ITEM) : PickItem()

    data class Header(val title: String) : PickItem()
}