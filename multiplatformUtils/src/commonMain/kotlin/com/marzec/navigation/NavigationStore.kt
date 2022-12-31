package com.marzec.navigation

import androidx.compose.runtime.Composable
import com.marzec.mvi.Store3
import com.marzec.preferences.Preferences
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class NavigationStore(
    private val router: (Destination) -> @Composable (destination: Destination, cacheKey: String) -> Unit,
    scope: CoroutineScope,
    private val stateCache: Preferences,
    private val resultCache: ResultCache,
    private val cacheKey: String,
    private val cacheKeyProvider: () -> String,
    initialState: NavigationState
) : Store3<NavigationState>(scope, stateCache.get(cacheKey) ?: initialState) {

    fun next(action: NavigationAction, requestId: Int? = null, secondaryId: Int? = null) =
        intent<Unit> {
            reducer {
                state.copy(
                    backStack = state.backStack.toMutableList().apply {
                        cleanResultCacheForCurrentScreen()
                        handlePoppingScreens(action)
                        addNextScreen(action, requestId, secondaryId)
                    }
                )
            }
        }

    fun goBack(result: Any? = null) = intent<Unit> {
        reducer {
            val requestKey = state.backStack.last().requestKey
            if (requestKey != null) {
                resultCache.save(requestKey, result)
            }
            state.copy(
                backStack = state.backStack.toMutableList().apply {
                    if (size > 1) {
                        removeLast().also {
                            stateCache.remove(it.cacheKey)
                            resultCache.remove(it.cacheKey)
                        }
                    }
                }
            )
        }
    }

    private fun MutableList<NavigationEntry>.addNextScreen(
        action: NavigationAction,
        requestId: Int?,
        secondaryId: Int?
    ) {
        val screenProvider = router(action.destination)
        add(
            NavigationEntry(
                destination = action.destination,
                cacheKey = cacheKeyProvider(),
                screenProvider = screenProvider,
                requestKey = requestId?.let {
                    RequestKey(
                        requesterKey = last().cacheKey,
                        requestId = requestId,
                        options = secondaryId?.let {
                            mapOf(SECONDARY_ID to it.toString())
                        } ?: emptyMap()
                    )
                }
            )
        )
    }

    private fun MutableList<NavigationEntry>.handlePoppingScreens(
        action: NavigationAction
    ) {
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
    }

    private suspend fun MutableList<NavigationEntry>.cleanResultCacheForCurrentScreen() {
        lastOrNull()?.cacheKey?.let { requesterKey -> resultCache.remove(requesterKey) }
    }

    @Suppress("unchecked_cast")
    suspend fun <T : Any> observe(requestId: Int): Flow<T>? =
        state.value.backStack.lastOrNull()?.let {
            resultCache.observe(it.cacheKey, requestId).map { cache -> cache?.data as? T }
                .filterNotNull()
        }

    suspend fun <T : Any> observeResult(requestId: Int): Flow<ResultValue<T>>? =
        state.value.backStack.lastOrNull()?.let { entry ->
            resultCache.observe(entry.cacheKey, requestId)
                .filterIsInstance<ResultCacheValue>()
                .filter { it.data != null }
                .map { ResultValue(it.requestKey.secondaryIdValue, it.data as T) }
        }

    override suspend fun onNewState(newState: NavigationState) {
        super.onNewState(newState)
        stateCache.set(cacheKey, newState)
    }
}

fun NavigationStore.next(destination: Destination) =
    next(NavigationAction(destination))

data class ResultValue<T>(
    val id: Int,
    val data: T
)