package com.marzec.todo.view.cache

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.remove
import androidx.datastore.preferences.core.toMutablePreferences
import com.marzec.todo.cache.FileCache
import com.marzec.todo.extensions.getMessage
import com.marzec.todo.extensions.toJsonElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement

class PreferencesCache(
    private val dataStore: DataStore<Preferences>
) : FileCache {
    override suspend fun put(key: String, value: Any?) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().also {
                if (value != null) {
                    it[preferencesKey(key)] = value
                } else {
                    it.remove(preferencesKey(key))
                }
            }
        }
    }

    override suspend fun get(key: String): JsonElement? {
        return try {
            dataStore.data.first()[preferencesKey(key)]?.toJsonElement()
        } catch (e: Exception) {
            Log.d("KURWA", e.getMessage())
            "".toJsonElement()
        }
    }

    override suspend fun observe(key: String): Flow<JsonElement?> {
        return dataStore.data.map {
            it[preferencesKey(key)]?.toJsonElement()
        }
    }
}