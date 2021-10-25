package com.marzec.todo.extensions

fun <T> List<T>.replaceIf(condition: (T) -> Boolean, replace: (T) -> T): List<T> = map { item: T ->
    if (condition(item)) {
        replace(item)
    } else {
        item
    }
}
