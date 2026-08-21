package com.marzec.navigation

import com.marzec.mvi.IntentContext
import com.marzec.mvi.Store4Impl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NavigationStore(
    scope: CoroutineScope,
    private val stateCache: NavigationStateCache,
    private val resultCache: ResultCache,
    private val cacheKey: String,
    private val cacheKeyProvider: () -> String,
    initialState: NavigationState,
    private val overrideLastClose: (NavigationState.() -> NavigationUpdate)? = null,
    private val onAfterClosed: ((entry: NavigationEntry) -> Unit)? = null
) : Store4Impl<NavigationState>(scope, initialState) {

    init {
        scope.launch {
            val cached = stateCache.read<NavigationState>(cacheKey)
            if (cached != null) {
                updateState(cached)
            } else {
                stateCache.write(cacheKey, state)
            }
        }
    }

    fun next(
        action: NavigationAction,
        requestId: Int? = null,
        secondaryId: Int? = null,
        id: String = ""
    ) =
        navigate(
            action = action,
            id = id,
            requestId = requestId,
            options = secondaryId?.let {
                mapOf<String, Any>(SECONDARY_ID to it)
            } ?: emptyMap()
        )

    fun nextWithOptionRequest(
        action: NavigationAction,
        requestId: Int? = null,
        options: Map<String, Any>? = null,
        id: String = ""
    ) = navigate(action, id, requestId, options)

    private fun navigate(
        action: NavigationAction,
        id: String,
        requestId: Int?,
        options: Map<String, Any>?
    ) = navigate {
        state.cleanResultCacheForCurrentScreen()

        val requestKey = requestKey(requestId, options)

        val poppedScreens = mutableListOf<NavigationEntry>()
        val newState = state.popScreens(action, poppedScreens)
            .addNextScreen(action, id, requestKey)

        NavigationUpdate(newState, poppedScreens)
    }

    private fun IntentContext<NavigationState, NavigationUpdate>.requestKey(
        requestId: Int?,
        options: Map<String, Any>?
    ): RequestKey? {
        val requesterKey = state.currentScreen()?.cacheKey
        return if (requestId != null && requesterKey != null) {
            RequestKey(
                requesterKey = requesterKey,
                requestId = requestId,
                options = options.orEmpty()
            )
        } else {
            null
        }
    }

    fun goBack(result: Any? = null) = navigate {
        state.backStack.currentScreen()?.requestKey?.let { requestKey ->
            resultCache.save(requestKey, result)
        }
        if (state.screenCount == 1 && overrideLastClose != null) {
            overrideLastClose.invoke(state)
        } else {
            val poppedScreens = mutableListOf<NavigationEntry>()
            val newState = state.popScreens(PopEntryTarget.ScreenCount(count = 1), poppedScreens)
            NavigationUpdate(newState, poppedScreens)
        }
    }

    fun closeFlow(result: Any? = null) = navigate {
        val currentFlow = state.currentFlow()
        currentFlow.backStack.firstOrNull()?.requestKey?.let { requestKey ->
            resultCache.save(requestKey, result)
        }
        if (currentFlow.isRootFlow() && overrideLastClose != null) {
            overrideLastClose.invoke(state)
        } else {
            val poppedScreens = mutableListOf<NavigationEntry>()
            val newState = state.popScreens(PopEntryTarget.ToFlow(currentFlow.id, popToInclusive = true), poppedScreens)
            NavigationUpdate(newState, poppedScreens)
        }
    }

    private fun navigate(stateTransform: suspend IntentContext<NavigationState, NavigationUpdate>.() -> NavigationUpdate) {
        intent<NavigationUpdate> {
            onTrigger {
                flowOf(stateTransform())
            }

            reducer { resultNonNull().newState }

            sideEffect {
                onAfterClosed?.let {
                    val update = resultNonNull()
                    update.closedEntries.forEach {
                        onAfterClosed.invoke(it)
                    }
                }
            }
        }
    }

    private fun NavigationState.addNextScreen(
        action: NavigationAction,
        id: String,
        requestKey: RequestKey?
    ): NavigationFlow = copy(
        backStack = backStack.addNextScreen(action, id, requestKey)
    )

    private fun List<NavigationEntry>.addNextScreen(
        action: NavigationAction,
        id: String,
        requestKey: RequestKey?
    ): List<NavigationEntry> = toMutableList().apply {
        val last = lastOrNull()
        if (last?.subFlow != null) {
            val newLast = last.copy(
                subFlow = last.subFlow.addNextScreen(action, id, requestKey)
            )
            remove(last)
            add(newLast)
        } else {
            add(createNavigationEntry(action, id, requestKey))
        }
    }

    private fun MutableList<NavigationEntry>.createNavigationEntry(
        action: NavigationAction,
        id: String,
        requestKey: RequestKey?
    ): NavigationEntry {
        val cacheKey = cacheKeyProvider()
        val subFlow = (action.destination as? SubFlow)?.let { subFlow ->
            NavigationFlow(
                backStack = listOf(
                    createNavigationEntry(
                        action = NavigationAction(subFlow.startDestination),
                        id = id,
                        requestKey = requestKey
                    )
                ),
                id = subFlow.id
            )
        }
        return NavigationEntry(
            destination = action.destination,
            cacheKey = cacheKey,
            id = id,
            requestKey = requestKey,
            subFlow = subFlow
        )
    }

    private fun isTargetDestination(
        flow: NavigationFlow,
        entry: NavigationEntry,
        target: PopEntryTarget,
        poppedEntries: MutableList<NavigationEntry>
    ) = when (target) {
        is PopEntryTarget.ScreenCount -> entry.subFlow == null && target.count <= poppedEntries.size
        is PopEntryTarget.ToDestination -> entry.destination == target.popTo
        is PopEntryTarget.ToFlow -> entry.subFlow?.id == target.id || !target.popToInclusive && entry.subFlow == null && flow.id == target.id
        is PopEntryTarget.ToId -> entry.id.takeIf { it.isNotBlank() } == target.id
    }

    private suspend fun NavigationState.popScreens(
        action: NavigationAction,
        poppedScreens: MutableList<NavigationEntry>
    ): NavigationFlow =
        popScreens(popEntryTarget = action.options?.popTo, poppedScreens)

    private suspend fun NavigationState.popScreens(
        popEntryTarget: PopEntryTarget?,
        poppedScreens: MutableList<NavigationEntry>
    ): NavigationFlow {
        val newBackStack = popEntryTarget?.let {
            backStack.toMutableList().apply {
                popScreens(
                    flow = this@popScreens,
                    popEntryTarget = it,
                    poppedScreens = poppedScreens
                )
            }
        } ?: backStack
        return copy(backStack = newBackStack)
    }

    private suspend fun MutableList<NavigationEntry>.popScreens(
        flow: NavigationFlow,
        popEntryTarget: PopEntryTarget,
        poppedScreens: MutableList<NavigationEntry>
    ): Boolean {
        while (size > 0) {
            val entry = last()
            val isTargetDestination =
                isTargetDestination(flow, entry, popEntryTarget, poppedScreens)
            when {
                isTargetDestination -> {
                    poppedScreens += entry
                    remove(entry)
                }
                else -> remove(entry)
            }
        }
        return poppedScreens.isNotEmpty()
    }

    private fun updateState(newState: NavigationState) {
        reduce { newState }
    }
}
