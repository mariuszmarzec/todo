package com.marzec.extensions

import kotlinx.atomicfu.locks.ReentrantLock

suspend inline fun <T> ReentrantLock.withSuspendLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
