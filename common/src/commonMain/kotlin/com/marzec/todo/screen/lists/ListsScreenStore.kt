package com.marzec.todo.screen.lists

import com.marzec.mvi.Store
import com.marzec.todo.preferences.Preferences

class ListsScreenStore(
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: ListsScreenState
) : Store<ListsScreenState, ListScreenActions>(
    stateCache.get(cacheKey) ?: initialState
) {
    init {
        addIntent<ListScreenActions.AddNewList> {
            sideEffect { println("+ Clicked") }
        }
    }

    override suspend fun onNewState(newState: ListsScreenState) {
        stateCache.set(cacheKey, newState)
    }
}