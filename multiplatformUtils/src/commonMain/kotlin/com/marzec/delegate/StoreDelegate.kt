package com.marzec.delegate

import com.marzec.mvi.Intent3
import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.IntentContext
import com.marzec.mvi.Store4
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
open class StoreDelegate<State : Any> {

    private lateinit var store: Store4<State>

    open fun init(store: Store4<State>) {
        this.store = store
    }

    protected fun <RESULT : Any> intent(@BuilderInference buildFun: IntentBuilder<State, RESULT>.() -> Unit): Unit =
        store.intent(buildFun)

    protected fun <Result : Any> run(intent: Intent3<State, Result>) {
        store.run(intent)
    }

    protected fun sideEffectIntent(
        func: suspend IntentContext<State, Unit>.() -> Unit
    ) = store.sideEffectIntent(func)
}

@Suppress("unchecked_cast")
fun <STATE : Any> Store4<STATE>.delegates(vararg delegates: Any) {
    delegates.forEach {
        (it as StoreDelegate<STATE>).init(this@delegates)
    }
}