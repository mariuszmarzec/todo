package com.marzec.preferences

import com.marzec.navigation.StateEditor
import com.marzec.navigation.StateReader

class StateCacheProxy(private val stateCache: StateCache) : StateReader, StateEditor {

    override suspend fun <T> read(key: String): T? = stateCache.get(key)

    override suspend fun <T> write(key: String, value: T?) {
        if (value != null) {
            stateCache.set(key, value)
        } else {
            stateCache.remove(key)
        }
    }

    override suspend fun remove(key: String) = stateCache.remove(key)
}
