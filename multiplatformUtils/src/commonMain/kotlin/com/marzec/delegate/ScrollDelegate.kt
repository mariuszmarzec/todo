package com.marzec.delegate

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.marzec.mvi.State
import com.marzec.mvi.Store3
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
fun rememberScrollState(store: ScrollDelegate): LazyListState {
    val store3 by rememberUpdatedState(store as Store3<*>)
    val updatedIdentifier by rememberUpdatedState(store3.identifier)

    val listState: LazyListState = remember(updatedIdentifier) {
        val state = store3.state.value as State<out WithScrollListState<*>>
        LazyListState(
            state.data?.scrollListState?.index ?: 0,
            state.data?.scrollListState?.offset ?: 0
        )
    }
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        store.onFinishedScrolling(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )
    }
    return listState
}

@Composable
fun rememberColumnScrollState(store: ScrollDelegate): ScrollState {
    val store3 by rememberUpdatedState(store as Store3<*>)
    val updatedIdentifier by rememberUpdatedState(store3.identifier)

    val listState: ScrollState = remember(updatedIdentifier) {
        val state = store3.state.value as State<out WithScrollListState<*>>
        ScrollState(
            state.data?.scrollListState?.offset ?: 0
        )
    }
    LaunchedEffect(listState.value) {
        store.onFinishedScrolling(
            0,
            listState.value
        )
    }
    return listState
}
