package com.marzec.todo.cache

import com.marzec.todo.extensions.toJsonElement
import com.marzec.todo.extensions.toJsonObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class FileCacheImpl(
    private val fileName: String,
    private val json: Json,
    private val memoryCache: MemoryCache
) : FileCache {

    // TODO polymorphism json serialization
    private val lock = ReentrantLock()
    private var initialized: AtomicBoolean = AtomicBoolean(false)

    override suspend fun put(key: String, value: Any?) {
        initIfNeeded()
        memoryCache.put(key, value.toJsonElement())
        updateFile()
    }

    override suspend fun get(key: String): JsonElement? {
        initIfNeeded()
        return memoryCache.get<JsonElement>(key)
    }


    override suspend fun observe(key: String): Flow<JsonElement?> {
        initIfNeeded()
        return memoryCache.observe<JsonElement>(key)
    }

    private suspend fun initIfNeeded() {
        if (initialized.compareAndSet(false, true)) {
            try {
                lock.lock()
                createFileIfDoesNotExist()
                initMemoryCache()
            } finally {

                lock.unlock()
            }
        }
    }

    private fun createFileIfDoesNotExist() {
        val file = File(fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
    }

    private suspend fun initMemoryCache() {
        val file = File(fileName)
        val content = file.readText().ifEmpty { "{}" }
        json.decodeFromString<Map<String, JsonElement?>>(content).forEach {
            memoryCache.put(it.key, it.value)
        }
    }

    private suspend fun updateFile() {
        try {
            lock.lock()
            val file = File(fileName)
            val newContent = JsonObject(memoryCache.toMap().toJsonObject()).toString()
            file.writeText(newContent)
        } finally {

            lock.unlock()
        }
    }
}
