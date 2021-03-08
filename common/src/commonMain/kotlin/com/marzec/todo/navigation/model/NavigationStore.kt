package com.marzec.todo.navigation.model

import com.marzec.mvi.Intent
import com.marzec.mvi.Store

class NavigationStore(initialState: NavigationState) :
    Store<NavigationState, NavigationActions>(initialState) {
    init {
        intents = mutableMapOf(
            NavigationActions.Next::class to Intent(
                reducer = { action, _, state: NavigationState ->
                    action as NavigationActions.Next
                    state.copy(
                        stack = state.stack.toMutableList().apply {
                            add(action.screenProvider)
                        }
                    )
                }
            ),
            NavigationActions.Back::class to Intent(
                reducer = { action, _, state: NavigationState ->
                    action as NavigationActions.Back
                    state.copy(
                        stack = state.stack.toMutableList().apply {
                            if (size > 0) removeLast()
                        }

                    )
                }
            )
        )
    }
}