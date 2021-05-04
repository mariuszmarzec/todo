package com.marzec.todo.navigation.model

import androidx.compose.runtime.Composable
import com.marzec.mvi.Store
import com.marzec.todo.preferences.Preferences
import kotlin.reflect.KClass

class NavigationStore(
    router: Map<KClass<out Destination>, @Composable (destination: Destination, cacheKey: String) -> Unit>,
    private val stateCache: Preferences,
    cacheKeyProvider: () -> String,
    initialState: NavigationState
) : Store<NavigationState, NavigationActions>(initialState) {
    init {
        addIntent<NavigationActions.Next> {
            reducer {
                state.copy(
                    backStack = state.backStack.toMutableList().apply {
                        action.options?.let { options ->
                            takeLastWhile { it.destination != options.popTo }.forEach {
                                if (it.destination != options.popTo) {
                                    remove(it)
                                }
                            }
                            if (options.popToInclusive && lastOrNull()?.destination == options.popTo) {
                                removeLast()
                            }
                        }
                        val cacheKey = cacheKeyProvider()
                        val screenProvider = router.getValue(action.destination::class)
                        add(NavigationEntry(action.destination, cacheKey, screenProvider))
                    }
                )
            }
        }
        addIntent<NavigationActions.Back> {
            reducer {
                state.copy(
                    backStack = state.backStack.toMutableList().apply {
                        if (size > 1) {
                            removeLast().also { stateCache.remove(it.cacheKey) }
                        }
                    }
                )
            }
        }
    }
}

suspend fun NavigationStore.goBack() = sendAction(NavigationActions.Back)

suspend fun NavigationStore.next(destination: Destination) = sendAction(NavigationActions.Next(destination))