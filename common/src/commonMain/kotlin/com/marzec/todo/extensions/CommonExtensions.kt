package com.marzec.todo.extensions

import com.marzec.todo.model.Task

fun emptyString() = ""

@Suppress("unchecked_cast")
inline fun <reified T: Any> Any.asInstance(action: T.() -> Unit) = (this as? T)?.action()

@Suppress("unchecked_cast")
inline fun <reified T: Any, R> Any.asInstanceAndReturn(action: T.() -> R) = (this as? T)?.action()

fun Task.urlToOpen(): String? = subTasks.firstOrNull()?.description?.urls()?.firstOrNull()
    ?: description.urls().firstOrNull()
