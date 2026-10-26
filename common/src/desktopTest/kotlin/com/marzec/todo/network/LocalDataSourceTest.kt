package com.marzec.todo.network

import com.marzec.cache.FileCache
import com.marzec.time.CurrentTimeUtil
import com.marzec.todo.api.SchedulerDto
import com.marzec.todo.api.TaskDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalDataSourceTest {

    @Test
    fun weeklySchedulerRunsOnSelectedWeekday() = runTest {
        val originalClock = CurrentTimeUtil.clock
        try {
            // Wednesday, 28 October 2026. The weekly schedule explicitly selects Wednesday (ISO 3).
            CurrentTimeUtil.setOtherTime(day = 28, month = 10, year = 2026)
            val source = LocalDataSource(InMemoryFileCache())
            source.init(
                listOf(
                    TaskDto(
                        id = 1,
                        description = "Weekly task",
                        addedTime = "2026-10-26T00:00:00",
                        modifiedTime = "2026-10-26T00:00:00",
                        parentTaskId = null,
                        subTasks = emptyList(),
                        isToDo = true,
                        priority = 0,
                        scheduler = SchedulerDto(
                            hour = 0,
                            minute = 0,
                            startDate = "2026-10-26T00:00:00",
                            daysOfWeek = listOf(3),
                            dayOfMonth = 0,
                            repeatCount = -1,
                            repeatInEveryPeriod = 1,
                            type = "Weekly"
                        )
                    )
                )
            )

            val tasks = source.getAll()

            assertEquals(2, tasks.size)
            assertEquals(1, tasks[0].id)
            assertEquals("2026-10-28T00:00:00", tasks[0].scheduler?.lastDate)
            assertEquals(2, tasks[1].id)
            assertNull(tasks[1].scheduler)
        } finally {
            CurrentTimeUtil.clock = originalClock
        }
    }
}

private class InMemoryFileCache : FileCache {
    private val json = Json { ignoreUnknownKeys = true }
    private val values = mutableMapOf<String, JsonElement>()

    override suspend fun <T : Any> put(key: String, value: T?, serializer: KSerializer<T>) {
        values[key] = value?.let { json.encodeToJsonElement(serializer, it) } ?: JsonNull
    }

    override suspend fun <T : Any> get(key: String, serializer: KSerializer<T>): T? =
        when (val value = values[key]) {
            null, JsonNull -> null
            else -> json.decodeFromJsonElement(serializer, value)
        }

    override suspend fun <T : Any> observe(key: String, serializer: KSerializer<T>): Flow<T?> =
        flowOf(get(key, serializer))

    override suspend fun <T : Any> update(
        key: String,
        update: (T?) -> T?,
        serializer: KSerializer<T>
    ) {
        put(key, update(get(key, serializer)), serializer)
    }
}
