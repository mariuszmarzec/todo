package com.marzec.screen.featuretoggle

import com.marzec.content.Content
import com.marzec.extensions.asInstance
import com.marzec.model.FeatureToggle
import com.marzec.model.NewFeatureToggle
import com.marzec.model.UpdateFeatureToggle
import com.marzec.model.toUpdate
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.reduceContentToLoadingWithNoChanges
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceDataWithContent
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.StateCache
import com.marzec.repository.FeatureToggleRepository
import kotlinx.coroutines.CoroutineScope


class FeatureToggleStore(
    scope: CoroutineScope,
    private val args: FeatureToggleDetails,
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
                        id?.let {
                            repository.observeById(it)
                        }
                    }
                }
                reducer {
                    result?.let {
                        state.reduceDataWithContent(
                            result = resultNonNull(),
                            defaultData = FeatureToggleState.new(
                                id = args.id,
                                name = args.name,
                                value = args.value
                            )
                        ) { result ->
                            copy(
                                id = result.data.id,
                                name = result.data.name,
                                value = result.data.value
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

    fun save() = intent<Content<FeatureToggle>>("save") {
        onTrigger {
            state.ifDataAvailable {
                if (toggle != null) {
                    repository.update(
                        id = toggle.id,
                        UpdateFeatureToggle(
                            name = toggle.name.toUpdate(name),
                            value = toggle.name.toUpdate(value)
                        )
                    )
                } else {
                    repository.create(
                        NewFeatureToggle(name, value)
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
            navigationStore.goBack()
        }
    }

    override suspend fun onNewState(newState: State<FeatureToggleState>) {
        stateCache.set(cacheKey, newState)
    }
}
