package com.marzec.extensions

fun <T> T.applyIf(condition: T.() -> Boolean, block: T.() -> Unit): T {
    if (condition()) {
        block()
    }
    return this
}