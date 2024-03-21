package com.marzec.cache

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.remove
import androidx.datastore.preferences.core.toMutablePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class PreferencesCache(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) : FileCache {

    override suspend fun <T: Any> put(key: String, value: T?, serializer: KSerializer<T>) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().also {
                if (value != null) {
                    it[preferencesKey<String>(key)] = json.encodeToString(serializer, value)
                } else {
                    it.remove(preferencesKey<String>(key))
                }
            }
        }
    }

    override suspend fun <T: Any> get(key: String, serializer: KSerializer<T>): T? {
        return try {
            dataStore.data.first()[preferencesKey<String>(key)]?.let {
                json.decodeFromString(serializer, it)
            }
        } catch (expected: Exception) {
            null
        }
    }

    override suspend fun <T : Any> observe(key: String, serializer: KSerializer<T>): Flow<T?> {
        return dataStore.data.map {
            it[preferencesKey<String>(key)]?.let { value ->
                json.decodeFromString(serializer, value)
            }
        }
    }

    override suspend fun <T : Any> update(
        key: String,
        update: (T?) -> T?,
        serializer: KSerializer<T>
    ) {
        dataStore.updateData { preferences ->
            preferences.toMutablePreferences().also { mutablePreferences ->
                val value = mutablePreferences[preferencesKey<String>(key)]?.let {
                    json.decodeFromString(serializer, it)
                }?.let {
                    update(it)
                }
                if (value != null) {
                    mutablePreferences[preferencesKey<String>(key)] = json.encodeToString(serializer, value)
                } else {
                    mutablePreferences.remove(preferencesKey<String>(key))
                }
            }
        }

    }
}
