package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.reduceData
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.extensions.EMPTY_STRING
import com.marzec.todo.view.SearchState

interface SearchDelegate {
    fun onSearchQueryChanged(query: String)
    fun clearSearch()
    fun activateSearch()
    fun onSearchFocusChanged(focused: Boolean)
}

interface WithSearch<DATA> {

    val search: SearchState

    fun copyWithSearch(search: SearchState): DATA
}

class SearchDelegateImpl<DATA : WithSearch<DATA>> : StoreDelegate<State<DATA>>(), SearchDelegate {

    override fun onSearchQueryChanged(query: String) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSearch(search = search.copy(value = query))
            }
        }
    }

    override fun clearSearch() = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSearch(
                    search = SearchState(
                        value = EMPTY_STRING,
                        focused = false
                    )
                )
            }
        }
    }

    override fun activateSearch() = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSearch(
                    search = SearchState(
                        value = EMPTY_STRING,
                        focused = true
                    )
                )
            }
        }
    }

    override fun onSearchFocusChanged(focused: Boolean) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSearch(search = search.copy(focused = focused))
            }
        }
    }
}