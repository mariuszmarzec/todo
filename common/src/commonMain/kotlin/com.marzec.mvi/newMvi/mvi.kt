package com.marzec.mvi.newMvi

import com.marzec.mvi.Intent
import com.marzec.todo.model.User
import com.marzec.todo.network.Content
import com.marzec.todo.screen.login.model.LoginViewState
import kotlin.jvm.JvmName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
open class Store2<State : Any>(defaultState: State) {

    private val actions = MutableStateFlow<Intent2<State, Any>>(Intent2(state = defaultState, result = null))

    private val _intentContextFlow =
        MutableStateFlow<Intent2<State, Any>>(Intent2(state = defaultState, result = null))

    private var _state = MutableStateFlow(defaultState)

    val state: StateFlow<State>
        get() = _state

    suspend fun init(scope: CoroutineScope) {
        scope.launch {
            actions.flatMapMerge { intent ->
                debug("actions flatMapMerge intent: $intent")
                (intent.onTrigger(_state.value) ?: flowOf(null)).map {
                    debug("actions onTrigger state: ${intent.state} result: $it")
                    intent.copy(result = it, sideEffect = intent.sideEffect)
                }
            }.collect {
                debug("actions collect intent: $it")
                _intentContextFlow.emit(it)
            }
        }
        scope.launch {
            _intentContextFlow
                .runningReduce { old, new ->
                    val reducedState = new.reducer(new.result, old.state!!)
                    debug("_intentContextFlow runningReduce state: $reducedState")
                    old.copy(state = reducedState, result = new.result, sideEffect = new.sideEffect)
                }.onEach {
                    debug("_intentContextFlow onEach intent: $it")
                    onNewState(it.state!!)
                    it.sideEffect?.invoke(it.result, it.state)
                }.collect {
                    debug("_intentContextFlow collect state: ${it.state}")
                    _state.emit(it.state!!)
                }
        }
    }

    suspend fun <Result: Any> intent(buildFun: IntentBuilder<State, Result>.() -> Unit) {
        actions.emit(IntentBuilder<State, Result>().apply { buildFun() }.build())
    }

    suspend fun sideEffectIntent(func: suspend IntentBuilder.IntentContext<State, Unit>.() -> Unit) {
        actions.emit(IntentBuilder<State, Unit>().apply { sideEffect(func) }.build())
    }

    open suspend fun onNewState(newState: State) = Unit

    fun debug(string: String) {
        if (false) {
            println(string)
        }
    }
}

data class Intent2<State, out Result : Any>(
    val onTrigger: suspend (stateParam: State) -> Flow<Result>? = { _ -> null },
    val reducer: suspend (result: Any?, stateParam: State) -> State = { _, stateParam -> stateParam },
    val sideEffect: (suspend (result: Any?, state: State) -> Unit)? = null,
    val state: State?,
    val result: Result?
) {
    fun resultNonNull(): Result = result!!
}

@Suppress("UNCHECKED_CAST")
class IntentBuilder<State: Any, Result: Any> {

    private var onTrigger: suspend (stateParam: State) -> Flow<Result>? = { _ -> null }
    private var reducer: suspend (result: Any?, stateParam: State) -> State = { _, stateParam -> stateParam }
    private var sideEffect: (suspend (result: Any?, state: State) -> Unit)? = null

    fun onTrigger(func: suspend IntentContext<State, Result>.() -> Flow<Result>? = { null }): IntentBuilder<State, Result> {
        onTrigger = { state ->
            IntentContext<State, Result>(state, null).func()
        }
        return this
    }
    fun reducer(func: suspend IntentContext<State, Result>.() -> State): IntentBuilder<State, Result> {
        reducer = { result: Any?, state ->
            val res = result as? Result
            IntentContext(state, res).func()
        }
        return this
    }

    fun sideEffect(func: suspend IntentContext<State, Result>.() -> Unit): IntentBuilder<State, Result> {
        sideEffect = { result: Any?, state ->
            val res = result as? Result
            IntentContext(state, res).func()
        }
        return this
    }

    fun build(): Intent2<State, Result> = Intent2(onTrigger, reducer, sideEffect, null, null)

    data class IntentContext<State, Result>(
        val state: State,
        val result: Result?
    ) {
        fun resultNonNull(): Result = result!!
    }
}
