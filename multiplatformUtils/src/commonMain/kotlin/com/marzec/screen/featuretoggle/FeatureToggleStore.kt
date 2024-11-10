package com.marzec.screen.featuretoggle

import com.marzec.content.Content
import com.marzec.content.ifDataSuspend
import com.marzec.extensions.asInstance
import com.marzec.model.FeatureToggle
import com.marzec.model.toNullableUpdate
import com.marzec.model.toUpdate
import com.marzec.mvi.IntentContext
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.reduceContentToLoadingWithNoChanges
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceDataWithContent
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationOptions
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.PopEntryTarget
import com.marzec.preferences.StateCache
import com.marzec.repository.FeatureToggleRepository
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.FeatureToggle
import com.marzec.todo.model.UpdateFeatureToggle
import com.marzec.todo.navigation.TodoDestination
import kotlinx.coroutines.CoroutineScope

class FeatureToggleStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: StateCache,
    private val initialState: State<FeatureToggleState>,
    private val repository: FeatureToggleRepository,
) : Store4Impl<State<FeatureToggleState>>(
    scope, stateCache.get(cacheKey) ?: initialState
) {

    fun initialLoad() = (stateCache.get(cacheKey) ?: initialState)
        .asInstance<State.Loading<FeatureToggleState>> {
            intent<Content<FeatureToggle>>("load") {
                onTrigger {
                    state.ifDataAvailable(blockOnLoading = false) {
                        FeatureToggleId?.let {
                            repository.observeFeatureToggle(it)
                        }
                    }
                }
                reducer {
                    result?.let {
                        state.reduceDataWithContent(
                            result = resultNonNull(),
                            defaultData = FeatureToggleState.default(
                                FeatureToggleId = 0,
                                parentFeatureToggleId = null
                            )
                        ) { result ->
                            copy(
                                FeatureToggleId = result.data.id,
                                FeatureToggle = result.data,
                                parentFeatureToggleId = result.data.parentFeatureToggleId,
                                description = result.data.description,
                                priority = result.data.priority,
                                isToDo = result.data.isToDo,
                                scheduler = result.data.scheduler,
                                highestPriorityAsDefault = result.data.scheduler?.highestPriorityAsDefault
                                    ?: Scheduler.HIGHEST_PRIORITY_AS_DEFAULT,
                                removeAfterSchedule = (result.data.scheduler as? Scheduler.OneShot)?.removeScheduled
                                    ?: Scheduler.REMOVE_SCHEDULED
                            )
                        }
                    } ?: state
                }
            }
        }
    
    fun onNameChanged(name: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(name = name) }
        }
    }

    fun onValueChanged(value: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(value = value) }
        }
    }

    fun FeatureToggle() = intent<Content<Unit>>("FeatureToggle") {
        onTrigger {
            state.ifDataAvailable {
                if (FeatureToggle != null) {
                    repository.updateFeatureToggle(
                        FeatureToggleId = FeatureToggle.id,
                        UpdateFeatureToggle(
                            description = description.toUpdate(FeatureToggle.description),
                            parentFeatureToggleId = parentFeatureToggleId.toNullableUpdate(FeatureToggle.parentFeatureToggleId),
                            priority = priority.toUpdate(FeatureToggle.priority),
                            isToDo = isToDo.toUpdate(FeatureToggle.isToDo),
                            scheduler = schedulerWithOptions.toNullableUpdate(FeatureToggle.scheduler)
                        )
                    )
                } else {
                    repository.FeatureToggle(
                        description,
                        parentFeatureToggleId,
                        highestPriorityAsDefault,
                        schedulerWithOptions
                    )
                }
            }
        }

        cancelTrigger(runSideEffectAfterCancel = true) {
            resultNonNull() is Content.Data
        }

        reducer {
            state.reduceContentToLoadingWithNoChanges(resultNonNull())
        }

        sideEffect {
            navigateOutAfterCall()
        }
    }

    fun addManyFeatureToggles() = intent<Content<Unit>>("addManyFeatureToggles") {
        onTrigger {
            state.ifDataAvailable {
                repository.FeatureToggles(
                    highestPriorityAsDefault = highestPriorityAsDefault,
                    parentFeatureToggleId = parentFeatureToggleId,
                    descriptions = description.split("\n").let {
                        if (highestPriorityAsDefault) {
                            it.reversed()
                        } else {
                            it
                        }
                    },
                    scheduler = schedulerWithOptions
                )
            }
        }

        cancelTrigger(runSideEffectAfterCancel = true) {
            resultNonNull() is Content.Data
        }

        reducer {
            state.reduceContentToLoadingWithNoChanges(resultNonNull())
        }

        sideEffect {
            navigateOutAfterCall()
        }
    }


    private suspend fun IntentContext<State<FeatureToggleState>, Content<Unit>>.navigateOutAfterCall() {
        result?.ifDataSuspend {
            state.ifDataAvailable(blockOnLoading = false) {
                val FeatureToggleIdToShow = FeatureToggleId ?: parentFeatureToggleId
                when {
                    FeatureToggleIdToShow != null -> {
                        val destination = TodoDestination.FeatureToggleDetails(FeatureToggleIdToShow)
                        navigationStore.next(
                            NavigationAction(
                                destination = destination,
                                options = NavigationOptions(
                                    PopEntryTarget.ToDestination(
                                        popTo = destination,
                                        popToInclusive = true
                                    )
                                )
                            )
                        )
                    }

                    else -> {
                        navigationStore.goBack()
                    }
                }
            }
        }
    }

    override suspend fun onNewState(newState: State<FeatureToggleState>) {
        stateCache.set(cacheKey, newState)
    }
}
