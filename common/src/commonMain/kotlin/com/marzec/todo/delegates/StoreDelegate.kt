package com.marzec.todo.delegates

import com.marzec.mvi.newMvi.IntentBuilder
import com.marzec.mvi.newMvi.Store2

open class StoreDelegate<State : Any> {

    private lateinit var store: Store2<State>

    open fun init(store: Store2<State>) {
        this.store = store
    }

    fun <RESULT : Any> intent(buildFun: IntentBuilder<State, RESULT>.() -> Unit) =
        store.intent(buildFun)

    fun sideEffectIntent(
        func: suspend IntentBuilder.IntentContext<State, Unit>.() -> Unit
    ) = store.sideEffectIntent(func)
}
