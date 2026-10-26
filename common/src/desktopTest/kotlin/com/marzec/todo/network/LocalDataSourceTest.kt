package com.marzec.todo.network

import com.marzec.cache.FileCache
import com.marzec.time.currentTime
import com.marzec.todo.api.SchedulerDto
import com.marzec.todo.api.TaskDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.datetime.isoDayNumber
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
        val today = currentTime()
        val todayDate = today.date.toString()
        val weekday = today.dayOfWeek.isoDayNumber
        val source = LocalDataSource(InMemoryFileCache())
        source.init(
            listOf(
                TaskDto(
                    id = 1,
                    description = "Weekly task",
                    addedTime = "${todayDate}T00:00:00",
                    modifiedTime = "${todayDate}T00:00:00",
                        parentTaskId = null,
                        subTasks = emptyList(),
                        isToDo = true,
                        priority = 0,
                        scheduler = SchedulerDto(
                            hour = 0,
                            minute = 0,
                            startDate = "${todayDate}T00:00:00",
                            daysOfWeek = listOf(weekday),
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
        assertEquals(todayDate, tasks[0].scheduler?.lastDate?.substringBefore("T"))
        assertEquals(2, tasks[1].id)
        assertNull(tasks[1].scheduler)
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
