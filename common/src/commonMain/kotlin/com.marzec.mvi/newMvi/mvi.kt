package com.marzec.mvi.newMvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.stateIn

@ExperimentalCoroutinesApi
open class Store2<State: Any>(defaultState: State) {

    private val actions = MutableStateFlow(Intent2<State, Any>(state = defaultState, result = null))

    private val _intentContextFlow = MutableStateFlow(Intent2<State, Any>(state = defaultState, result = null))

    private lateinit var _state: StateFlow<State>

    val state: StateFlow<State>
        get() = _state

    suspend fun init(scope: CoroutineScope) {

        actions.flatMapMerge { intent ->
            intent.onTrigger(_state.value).map {
                intent.copy(result = it)
            }
        }.collect {
            _intentContextFlow.emit(it)
        }

        _state = _intentContextFlow
            .runningReduce { old, new ->
                old.copy(state = new.reducer(new.result, old.state!!), sideEffect = new.sideEffect)

            }.onEach {
                onNewState(it.state!!)
                it.sideEffect?.invoke(it.result, it.state!!)
            }.map {
                it.state!!
            }
            .stateIn(scope)
    }

    suspend fun intent(intent: Intent2<State, Any>) {
        actions.emit(intent)
    }

    open suspend fun onNewState(newState: State) = Unit
}

data class Intent2<State, Result>(
    val onTrigger: (stateParam: State) -> Flow<Result?> = { _ -> flowOf(null) },
    val reducer: suspend (result: Result?, stateParam: State) -> State = { _, stateParam -> stateParam },
    val sideEffect: (suspend (result: Result?, state: State) -> Unit)? = null,
    val state: State?,
    val result: Result?
) {
    fun resultNonNull(): Result = result!!
}

// TODO combine actions with latest state