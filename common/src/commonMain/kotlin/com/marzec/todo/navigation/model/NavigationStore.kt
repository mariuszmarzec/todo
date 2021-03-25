package com.marzec.todo.navigation.model

import androidx.compose.runtime.Composable
import com.marzec.mvi.Store
import com.marzec.todo.preferences.Preferences

class NavigationStore(
    router: Map<Destinations, @Composable (cacheKey: String) -> Unit>,
    private val stateCache: Preferences,
    cacheKeyProvider: () -> String,
    initialState: NavigationState
) : Store<NavigationState, NavigationActions>(initialState) {
    init {
        addIntent<NavigationActions.Next> {
            reducer {
                state.copy(
                    backStack = state.backStack.toMutableList().apply {
                        val cacheKey = cacheKeyProvider()
                        val screenProvider = router.getValue(action.destination)
                        add(NavigationEntry(cacheKey, screenProvider))
                    }
                )
            }
        }
        addIntent<NavigationActions.Back> {
            reducer {
                state.copy(
                    backStack = state.backStack.toMutableList().apply {
                        if (size > 0) {
                            removeLast().also { stateCache.remove(it.cacheKey) }
                        }
                    }
                )
            }
        }
    }
}