package com.marzec.delegate

import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.IntentContext
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
        func: suspend IntentContext<State, Unit>.() -> Unit
    ) = store.sideEffect(func)
}

@Suppress("unchecked_cast")
fun <STATE : Any> Store3<STATE>.delegates(vararg delegates: Any) {
    delegates.forEach {
        (it as StoreDelegate<STATE>).init(this@delegates)
    }
}