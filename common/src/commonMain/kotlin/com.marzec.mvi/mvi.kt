package com.marzec.mvi

import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
open class Store<State: Any, Action : Any>(defaultState: State) {
    
    var intents = mapOf<KClass<out Any>, Intent<State>>()

    private val actions = BroadcastChannel<Action>(-1)

    private val _state = MutableStateFlow(defaultState)

    val state: StateFlow<State>
        get() = _state

    @ObsoleteCoroutinesApi
    suspend fun sendAction(action: Action) {
        actions.consume {
            println(action)
            val intent = intents[action::class]
            requireNotNull(intent)
            val result = intent.onTrigger?.invoke(action, _state.value)

            val newState = intent.reducer(action, result, _state.value)

            _state.value = newState
            onNewState(newState)

            intent.sideEffect?.invoke(action, result, _state.value)

        }
        actions.send(action)
    }

    open suspend fun onNewState(newState: State) = Unit

    inline fun <reified IntentAction : Any, Result: Any> addIntent(noinline buildFun: IntentBuilder<State, IntentAction, Result>.() -> Unit) {
        intents = intents + mapOf<KClass<out Any>, Intent<State>>(IntentAction::class to intent(buildFun))
    }

    @JvmName("addIntentNoResult")
    inline fun <reified IntentAction : Any> addIntent(noinline buildFun: IntentBuilder<State, IntentAction, Any>.() -> Unit) {
        intents = intents + mapOf<KClass<out Any>, Intent<State>>(IntentAction::class to intent(buildFun))
    }
}

data class Intent<State>(
    val onTrigger: (suspend (action: Any, state: State) -> Any?)? = null,
    val reducer: suspend (action: Any, result: Any?, state: State) -> State = {_, _, state -> state},
    val sideEffect: (suspend (action: Any, result: Any?, state: State) -> Unit)? = null
)

@Suppress("UNCHECKED_CAST")
class IntentBuilder<State: Any, Action: Any, Result: Any> {

    private var onTrigger: (suspend (action: Any, state: State) -> Any?)? = null
    private var reducer: suspend (action: Any, result: Any?, state: State) -> State = {_, _, state -> state}
    private var sideEffect: (suspend (action: Any, result: Any?, state: State) -> Unit)? = null

    fun onTrigger(func: suspend IntentContext<State, Action, Result>.() -> Result?): IntentBuilder<State, Action, Result> {
        onTrigger = { action: Any, state ->
            action as Action
            IntentContext<State, Action, Result>(action, state, null).func()
        }
        return this
    }
    fun reducer(func: suspend IntentContext<State, Action, Result>.() -> State): IntentBuilder<State, Action, Result> {
        reducer = { action: Any, result: Any?, state ->
            action as Action
            val res = result as? Result
            IntentContext(action, state, res).func()
        }
        return this
    }

    fun sideEffect(func: suspend IntentContext<State, Action, Result>.() -> Unit): IntentBuilder<State, Action, Result> {
        sideEffect = { action: Any, result: Any?, state ->
            action as Action
            val res = result as? Result
            IntentContext(action, state, res).func()
        }
        return this
    }

    fun build(): Intent<State> = Intent(onTrigger, reducer, sideEffect)

    data class IntentContext<State, Action, Result>(
        val action: Action,
        val state: State,
        val result: Result?
    ) {
        fun resultNonNull(): Result = result!!
    }
}

fun <State: Any, Action: Any, Result: Any> intent(
    buildFun: IntentBuilder<State, Action, Result>.() -> Unit
): Intent<State> = with(IntentBuilder<State, Action, Result>()) {
    buildFun().run { build() }
}

@JvmName("intentNoResult")
fun <State: Any, Action: Any> intent(
    buildFun: IntentBuilder<State, Action, Any>.() -> Unit
): Intent<State> = with(IntentBuilder<State, Action, Any>()) {
    buildFun().run { build() }
} 