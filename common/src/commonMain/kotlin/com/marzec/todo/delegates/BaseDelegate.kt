package com.marzec.todo.delegates

import com.marzec.mvi.newMvi.Intent2
import com.marzec.mvi.newMvi.IntentBuilder

open class BaseDelegate<State : Any> {

    fun <RESULT : Any> intent(buildFun: IntentBuilder<State, RESULT>.() -> Unit): Intent2<State, RESULT> =
        IntentBuilder<State, RESULT>().apply { buildFun() }.build()
}
