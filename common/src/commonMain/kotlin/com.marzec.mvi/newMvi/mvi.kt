package com.marzec.mvi.newMvi

import com.marzec.todo.DI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
open class Store2<State : Any>(private val defaultState: State) {

    private val actions =
        MutableSharedFlow<Intent2<State, Any>>(extraBufferCapacity = 10)
    private val _intentContextFlow =
        MutableStateFlow<Intent2<State, Any>>(Intent2(state = defaultState, result = null))

    private var _state = MutableStateFlow(defaultState)

    val state: StateFlow<State>
        get() = _state

    private var pause = MutableStateFlow(false)

    suspend fun init(scope: CoroutineScope, initialAction: suspend () -> Unit = {}) {
        pause.emit(false)
        scope.launch {
            actions.onSubscription { initialAction() }
                .flatMapMerge { intent ->
                debug("actions flatMapMerge intent: $intent")
                val currentState = _state.value
                val flow = intent.onTrigger(currentState)
                    ?.makeCancellableIfNeeded(
                        intent.isCancellableFlowTrigger
                    ) ?: flowOf(null)
                flow.map {
                    debug("actions onTrigger state: ${intent.state} result: $it")
                    intent.copy(state = currentState, result = it, sideEffect = intent.sideEffect)
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

    private fun <T> Flow<T>.makeCancellableIfNeeded(
        isCancellableFlowTrigger: Boolean
    ) = if (isCancellableFlowTrigger) {
        combine(pause) { triggerValue, pause ->
            pause to triggerValue
        }.filter { !it.first }
            .map { it.second }
    } else {
        this
    }

    protected fun <T> Flow<T>.cancelFlowsIf(function: (T) -> Boolean): Flow<T> =
        onEach {
            if (function.invoke(it)) {
                cancelFlows()
            }
        }

    protected suspend fun cancelFlows() {
        pause.emit(true)
    }

    suspend fun <Result : Any> intent(buildFun: IntentBuilder<State, Result>.() -> Unit) {
        actions.emit(IntentBuilder<State, Result>().apply { buildFun() }.build())
    }

    suspend fun <Result : Any> delegate(intent: Intent2<State, Result>) {
        actions.emit(intent)
    }

    suspend fun sideEffectIntent(func: suspend IntentBuilder.IntentContext<State, Unit>.() -> Unit) {
        actions.emit(IntentBuilder<State, Unit>().apply { sideEffect(func) }.build())
    }

    open suspend fun onNewState(newState: State) = Unit

    fun debug(string: String) {
        if (false) {
            DI.logger.log("MVI-${defaultState.javaClass.simpleName}", string)
        }
    }
}

data class Intent2<State, out Result : Any>(
    val onTrigger: suspend (stateParam: State) -> Flow<Result>? = { _ -> null },
    val reducer: suspend (result: Any?, stateParam: State) -> State = { _, stateParam -> stateParam },
    val sideEffect: (suspend (result: Any?, state: State) -> Unit)? = null,
    val state: State?,
    val result: Result?,
    val isCancellableFlowTrigger: Boolean = false
) {
    fun resultNonNull(): Result = result!!
}

@Suppress("UNCHECKED_CAST")
class IntentBuilder<State : Any, Result : Any> {

    private var onTrigger: suspend (stateParam: State) -> Flow<Result>? = { _ -> null }
    private var reducer: suspend (result: Any?, stateParam: State) -> State =
        { _, stateParam -> stateParam }
    private var sideEffect: (suspend (result: Any?, state: State) -> Unit)? = null
    private var isCancellableFlowTrigger: Boolean = false

    fun onTrigger(
        isCancellableFlowTrigger: Boolean = false,
        func: suspend IntentContext<State, Result>.() -> Flow<Result>? = { null }
    ): IntentBuilder<State, Result> {
        this.isCancellableFlowTrigger = isCancellableFlowTrigger
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

    fun build(): Intent2<State, Result> = Intent2(
        onTrigger = onTrigger,
        reducer = reducer,
        sideEffect = sideEffect,
        state = null,
        result = null,
        isCancellableFlowTrigger = isCancellableFlowTrigger
    )

    data class IntentContext<State, Result>(
        val state: State,
        val result: Result?
    ) {
        fun resultNonNull(): Result = result!!
    }
}

fun <State : Any, Result : Any> IntentBuilder<State, Result>.oneShotTrigger(
    action: suspend IntentBuilder.IntentContext<State, Result>.() -> Result
): IntentBuilder<State, Result> {
    onTrigger {
        flow {
            emit(action())
        }
    }
    return this
}
