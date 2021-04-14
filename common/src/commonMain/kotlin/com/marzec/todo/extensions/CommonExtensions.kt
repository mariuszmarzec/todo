package com.marzec.todo.extensions

@Suppress("unchecked_cast")
fun <T: Any> Any.asInstance(action: T.() -> Unit) = (this as? T)?.action()

@Suppress("unchecked_cast")
suspend fun <T: Any> Any.asInstanceSuspend(action: suspend T.() -> Unit) = (this as? T)?.action()