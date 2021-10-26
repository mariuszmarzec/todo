package com.marzec.todo.delegates

import com.marzec.mvi.newMvi.Intent2
import com.marzec.mvi.newMvi.IntentBuilder

open class StoreDelegate<State : Any> {

    fun <RESULT : Any> intent(buildFun: IntentBuilder<State, RESULT>.() -> Unit): Intent2<State, RESULT> =
        IntentBuilder<State, RESULT>().apply { buildFun() }.build()

    fun sideEffectIntent(
        func: suspend IntentBuilder.IntentContext<State, Unit>.() -> Unit
    ): Intent2<State, Unit> = IntentBuilder<State, Unit>().apply {
        sideEffect(func)
    }.build()
}
