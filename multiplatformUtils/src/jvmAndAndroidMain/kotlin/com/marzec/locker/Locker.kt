package com.marzec.locker

import java.util.concurrent.locks.ReentrantLock

actual class Locker {

    private val lock = ReentrantLock()

    actual fun lock() {
        lock.lock()
    }

    actual fun unlock() {
        lock.unlock()
    }
}