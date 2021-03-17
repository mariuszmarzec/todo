package com.marzec.todo.navigation.model

import com.marzec.mvi.Store

class NavigationStore(initialState: NavigationState) :
    Store<NavigationState, NavigationActions>(initialState) {
    init {
        addIntent<NavigationActions.Next> {
            reducer {
                state.copy(
                    stack = state.stack.toMutableList().apply {
                        add(action.screenProvider)
                    }
                )
            }
        }
        addIntent<NavigationActions.Back> {
            reducer {
                state.copy(
                    stack = state.stack.toMutableList().apply {
                        if (size > 0) removeLast()
                    }
                )
            }
        }
    }
}