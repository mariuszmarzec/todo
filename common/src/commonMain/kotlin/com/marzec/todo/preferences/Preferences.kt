package com.marzec.todo.preferences

interface Preferences {
    suspend fun set(key: String, value: String)
    suspend fun getString(key: String): String?
    suspend fun remove(key: String)
}

class MemoryPreferences : Preferences {

    private val map = HashMap<String, Any>()

    override suspend fun set(key: String, value: String) {
        map[key] = value
    }

    override suspend fun getString(key: String): String? = map[key] as? String

    override suspend fun remove(key: String) {
        map.remove(key)
    }
}