package com.marzec.navigation

import androidx.compose.runtime.Composable
import com.marzec.mvi.IntentContext
import com.marzec.mvi.Store3
import com.marzec.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
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
        navigate {
            state.copy(
                backStack = state.backStack.toMutableList().apply {
                    cleanResultCacheForCurrentScreen()
                    handlePoppingScreens(action)
                    addNextScreen(action, requestId, secondaryId)
                }
            )
        }

    fun nextWithOptionRequest(
        action: NavigationAction,
        requestId: Int? = null,
        options: Map<String, Any>? = null
    ) = navigate {
        state.copy(
            backStack = state.backStack.toMutableList().apply {
                cleanResultCacheForCurrentScreen()
                handlePoppingScreens(action)
                addNextScreen(action, requestId, options)
            }
        )
    }

    fun goBack(result: Any? = null) = navigate {
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

    private fun navigate(stateTransform: suspend IntentContext<NavigationState, NavigationState>.() -> NavigationState) {
        intent {
            onTrigger {
                flowOf(stateTransform())
            }

            reducer { resultNonNull() }
        }
    }

    private fun MutableList<NavigationEntry>.addNextScreen(
        action: NavigationAction,
        requestId: Int?,
        secondaryId: Int?
    ) {
        val options = secondaryId?.let {
            mapOf<String, Any>(SECONDARY_ID to it)
        } ?: emptyMap()
        addNextScreen(action, requestId, options)
    }

    private fun MutableList<NavigationEntry>.addNextScreen(
        action: NavigationAction,
        requestId: Int?,
        options: Map<String, Any>?
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
                        options = options.orEmpty()
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
                .filter { it.data != null && it.requestKey.options.isNotEmpty() }
                .map {
                    ResultValue(
                        requestId = it.requestKey.requestId,
                        data = it.data as T,
                        options = it.requestKey.options
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

data class ResultValue<T>(
    val requestId: Int,
    val options: Map<String, Any> = emptyMap(),
    val data: T
)

val ResultValue<*>.secondaryIdValue: Any
    get() = options.getValue(SECONDARY_ID)