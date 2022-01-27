package com.marzec.todo.delegates

import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.Store3
import kotlin.experimental.ExperimentalTypeInference

open class StoreDelegate<State : Any> {

    private lateinit var store: Store3<State>

    open fun init(store: Store3<State>) {
        this.store = store
    }

    @OptIn(ExperimentalTypeInference::class)
    fun <RESULT : Any> intent(@BuilderInference buildFun: IntentBuilder<State, RESULT>.() -> Unit) =
        store.intent(buildFun)

    fun sideEffect(
        func: suspend IntentBuilder.IntentContext<State, Unit>.() -> Unit
    ) = store.sideEffect(func)
}
