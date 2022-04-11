package com.marzec.screen.pickitemscreen

import androidx.compose.runtime.Composable
import com.marzec.content.Content
import com.marzec.delegate.SelectionDelegate
import com.marzec.delegate.delegates
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.mvi.reduceDataWithContent
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

data class PickItemOptions<ITEM>(
    val loadData: suspend () -> Flow<Content<List<ITEM>>>,
    val mapItemToId: (ITEM) -> String,
    val itemRow: @Composable (item: ITEM, onItemClick: (ITEM) -> Unit) -> Unit,
    val onAddNavigationAction: (() -> NavigationAction)? = null,
    val multipleChoice: Boolean = false,
    val selected: Set<String> = emptySet()
)

const val RESULT_PICKER_ITEM_ID = "RESULT_PICKER_ITEM_ID"

class PickItemDataStore<ITEM>(
    private val options: PickItemOptions<ITEM>,
    private val navigationStore: NavigationStore,
    scope: CoroutineScope,
    private val stateCache: Preferences,
    initialState: State<PickItemData<ITEM>>,
    private val cacheKey: String,
    private val selectionDelegate: SelectionDelegate<String>
) : Store3<State<PickItemData<ITEM>>>(
    scope, stateCache.get(cacheKey) ?: initialState
), SelectionDelegate<String> by selectionDelegate {

    init {
        delegates(selectionDelegate)
    }

    fun load() = intent<Content<List<ITEM>>> {
        onTrigger {
            options.loadData()
        }

        reducer {
            state.reduceDataWithContent(resultNonNull()) {
                this?.copy(items = it) ?: PickItemData.default(options)
            }
        }
    }

    fun onItemClick(id: String) = sideEffect {
        navigationStore.goBack(RESULT_PICKER_ITEM_ID to id)
    }

    fun onConfirmClick() = sideEffect {
        state.ifDataAvailable {
            navigationStore.goBack(RESULT_PICKER_ITEM_ID to selected.toList())
        }
    }

    fun onAddButtonClick() = options.onAddNavigationAction?.let { action ->
        sideEffect {
            navigationStore.next(action())
        }
    }

    override suspend fun onNewState(newState: State<PickItemData<ITEM>>) {
        super.onNewState(newState)
        stateCache.set(cacheKey, newState)
    }
}
