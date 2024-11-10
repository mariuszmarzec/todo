package com.marzec.screen.pickitemscreen

import androidx.compose.runtime.Composable
import com.marzec.content.Content
import com.marzec.delegate.ScrollDelegate
import com.marzec.delegate.ScrollDelegateImpl
import com.marzec.delegate.SearchDelegate
import com.marzec.delegate.SearchDelegateImpl
import com.marzec.delegate.SelectionDelegate
import com.marzec.delegate.SelectionDelegateImpl
import com.marzec.mvi.delegates
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.reduceDataWithContent
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.StateCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

data class PickItemOptions<ITEM : Any>(
    val loadData: suspend () -> Flow<Content<List<ITEM>>>,
    val mapItemToId: (ITEM) -> String,
    val itemRow: @Composable (item: ITEM, onItemClick: (ITEM) -> Unit) -> Unit,
    val onAddNavigationAction: (() -> NavigationAction)? = null,
    val multipleChoice: Boolean = false,
    val returnIdOnly: Boolean = true,
    val selected: Set<String> = emptySet(),
    val stringsToCompare: ((ITEM) -> List<String>)? = null,
    val groupByHeader: ((ITEM) -> String)? = null
)

fun <ITEM : Any> providePickItemStore(
    navigationStore: NavigationStore,
    options: PickItemOptions<ITEM>,
    scope: CoroutineScope,
    stateCache: StateCache,
    cacheKey: String
): PickItemDataStore<ITEM> = PickItemDataStore(
    options = options,
    scope = scope,
    navigationStore = navigationStore,
    stateCache = stateCache,
    initialState = PickItemData.initial(options),
    cacheKey = cacheKey,
    selectionDelegate = SelectionDelegateImpl<String, PickItemData<ITEM>>(),
    searchDelegate = SearchDelegateImpl<PickItemData<ITEM>>(),
    scrollDelegate = ScrollDelegateImpl<PickItemData<ITEM>>()
)

class PickItemDataStore<ITEM : Any>(
    private val options: PickItemOptions<ITEM>,
    private val navigationStore: NavigationStore,
    scope: CoroutineScope,
    private val stateCache: StateCache,
    initialState: State<PickItemData<ITEM>>,
    private val cacheKey: String,
    private val selectionDelegate: SelectionDelegate<String>,
    private val searchDelegate: SearchDelegate,
    private val scrollDelegate: ScrollDelegate
) : Store4Impl<State<PickItemData<ITEM>>>(
    scope, stateCache.get(cacheKey) ?: initialState
), SelectionDelegate<String> by selectionDelegate,
    SearchDelegate by searchDelegate,
    ScrollDelegate by scrollDelegate {

    init {
        delegates(selectionDelegate, searchDelegate, scrollDelegate)
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

    fun onItemClick(item: ITEM) {
        sideEffectIntent {
            if (options.returnIdOnly) {
                navigationStore.goBack(options.mapItemToId(item))
            } else {
                state.ifDataAvailable {
                    navigationStore.goBack(item)
                }
            }
        }
    }

    fun onConfirmClick() = sideEffectIntent {
        state.ifDataAvailable {
            if (options.returnIdOnly) {
                navigationStore.goBack(selected.toList())
            } else {
                val selectedItems = items.filter { options.mapItemToId(it) in selected }
                navigationStore.goBack(selectedItems)
            }
        }
    }

    fun onAddButtonClick() = options.onAddNavigationAction?.let { action ->
        sideEffectIntent {
            navigationStore.next(action())
        }
    }

    override suspend fun onNewState(newState: State<PickItemData<ITEM>>) {
        super.onNewState(newState)
        stateCache.set(cacheKey, newState)
    }
}
