package com.marzec.navigation

import androidx.compose.runtime.Composable
import com.marzec.mvi.Store3
import com.marzec.preferences.Preferences
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope

class NavigationStore(
    private val router: Map<KClass<out Destination>, @Composable (destination: Destination, cacheKey: String) -> Unit>,
    scope: CoroutineScope,
    private val stateCache: Preferences,
    private val cacheKey: String,
    private val cacheKeyProvider: () -> String,
    initialState: NavigationState
) : Store3<NavigationState>(scope, stateCache.get(cacheKey) ?: initialState) {

    fun next(action: NavigationAction) = intent<Unit> {
        reducer {
            state.copy(
                backStack = state.backStack.toMutableList().apply {
                    action.options?.let { options ->
                        takeLastWhile { it.destination != options.popTo }.forEach {
                            if (it.destination != options.popTo) {
                                remove(it)
                                stateCache.remove(it.cacheKey)
                            }
                        }
                        if (options.popToInclusive && lastOrNull()?.destination == options.popTo) {
                            removeLast().also { stateCache.remove(it.cacheKey) }
                        }
                    }
                    val screenProvider = router.getValue(action.destination::class)
                    add(NavigationEntry(action.destination, cacheKeyProvider(), screenProvider))
                }
            )
        }
    }

    fun goBack() = intent<Unit> {
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

    override suspend fun onNewState(newState: NavigationState) {
        super.onNewState(newState)
        stateCache.set(cacheKey, newState)
    }
}

fun NavigationStore.next(destination: Destination) =
    next(NavigationAction(destination))