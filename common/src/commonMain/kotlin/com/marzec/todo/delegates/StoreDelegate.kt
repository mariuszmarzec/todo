package com.marzec.todo.delegates

import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.Store3

open class StoreDelegate<State : Any> {

    private lateinit var store: Store3<State>

    open fun init(store: Store3<State>) {
        this.store = store
    }

    fun <RESULT : Any> intent(buildFun: IntentBuilder<State, RESULT>.() -> Unit) =
        store.intent(buildFun)

    fun sideEffect(
        func: suspend IntentBuilder.IntentContext<State, Unit>.() -> Unit
    ) = store.sideEffect(func)
}
