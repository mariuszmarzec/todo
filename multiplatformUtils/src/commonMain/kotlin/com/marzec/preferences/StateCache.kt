package com.marzec.preferences

import com.marzec.navigation.StateEditor
import com.marzec.navigation.StateReader

interface StateCache : StateReader, StateEditor {
    fun set(key: String, value: Any)
    fun <T> get(key: String): T?
    fun remove(key: String)
}

class MemoryStateCache : StateCache {

    private val map = HashMap<String, Any>()

    override suspend fun <T> read(key: String): T? = get(key)

    override suspend fun write(key: String, value: Any?) {
        if (value != null) {
            set(key, value)
        } else {
            remove(key)
        }
    }

    override suspend fun remove(key: String) {
        map.remove(key)
    }

    override fun set(key: String, value: Any) {
        map[key] = value
    }

    @Suppress("unchecked_cast")
    override fun <T> get(key: String): T? = map[key] as? T
}
