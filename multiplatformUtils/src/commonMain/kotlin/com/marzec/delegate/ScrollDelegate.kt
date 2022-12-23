package com.marzec.delegate

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.marzec.mvi.State
import com.marzec.mvi.reduceData

interface ScrollDelegate {

    fun onFinishedScrolling(firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int)
}

class ScrollDelegateImpl<DATA : WithScrollListState<DATA>>
    : StoreDelegate<State<DATA>>(), ScrollDelegate {

    override fun onFinishedScrolling(
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int
    ) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithScrollListState(
                    scrollListState = scrollListState.copy(
                        index = firstVisibleItemIndex,
                        offset = firstVisibleItemScrollOffset
                    )
                )
            }
        }
    }
}

interface WithScrollListState<DATA> {

    val scrollListState: ScrollListState

    fun copyWithScrollListState(scrollListState: ScrollListState): DATA
}

data class ScrollListState(
    val index: Int = 0,
    val offset: Int = 0
) {
    companion object {
        val DEFAULT: ScrollListState = ScrollListState(index = 0, offset = 0)
    }
}

@Composable
fun <DATA> rememberScrollState(
    state: State<out WithScrollListState<DATA>>,
    store: ScrollDelegate
): LazyListState {
    val listState: LazyListState = rememberLazyListState(
        state.data?.scrollListState?.index ?: 0,
        state.data?.scrollListState?.offset ?: 0
    )

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        store.onFinishedScrolling(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )
    }
    return listState
}
