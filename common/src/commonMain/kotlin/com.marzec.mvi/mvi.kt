package com.marzec.mvi

import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class Store<State, Action : Any>(defaultState: State) {
    
    var intents = mapOf<KClass<out Action>, Intent<State>>()

    private val _state = MutableStateFlow(defaultState)

    val state: StateFlow<State>
        get() = _state

    fun sendAction(action: Action, scope: CoroutineScope) {
        scope.launch {
            val intent = intents[action::class]
            requireNotNull(intent)

            val result = intent.onTrigger?.invoke()

            val newState = intent.reducer(action, result, _state.value)

            _state.value = newState

            intent.sideEffect?.invoke(result, _state.value)
        }
    }
}


data class Intent<State>(
    val onTrigger: (suspend () -> Any?)? = null,
    val reducer: suspend (Any, Any?, State) -> State = {_, _, state -> state},
    val sideEffect: ((Any?, State) -> Unit)? = null
)