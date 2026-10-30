package com.marzec.todo.network

import com.marzec.cache.FileCache
import com.marzec.cache.MemoryCache
import com.marzec.logger.Logger
import com.marzec.logger.MultiLogger
import com.marzec.content.Content
import com.marzec.repository.LoginRepositoryMock
import com.marzec.time.currentTime
import com.marzec.todo.api.SchedulerDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.repository.TodoRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.isoDayNumber
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
        val today = currentTime()
        val todayDate = today.date.toString()
        // The API serializes weekdays as ISO 1-7; exercise that same representation here.
        val weekday = today.dayOfWeek.isoDayNumber
        val source = LocalDataSource(InMemoryFileCache())
        source.init(listOf(weeklyTask(todayDate, weekday)))

        val tasks = source.getAll()

        assertEquals(2, tasks.size)
        assertEquals(1, tasks[0].id)
        assertEquals(todayDate, tasks[0].scheduler?.lastDate?.substringBefore("T"))
        assertEquals(2, tasks[1].id)
        assertNull(tasks[1].scheduler)
    }

    @Test
    fun addNewTasksReturnsCreatedChildDtoAndRefreshesParentTree() = runTest {
        // Repository cache refreshes can log during the test; install a no-op test logger.
        Logger.logger = MultiLogger(emptyList())
        val fileCache = InMemoryFileCache()
        val dataSource = LocalDataSource(fileCache)
        dataSource.init(listOf(rootTask()))
        val client = HttpClient(CIO)
        val repository = TodoRepository(
            dataSource = dataSource,
            memoryCache = MemoryCache(),
            fileCache = fileCache,
            dispatcher = Dispatchers.Unconfined,
            client = client,
            loginRepository = LoginRepositoryMock()
        )

        try {
            val result = repository.addNewTasks(
                highestPriorityAsDefault = false,
                parentTaskId = 1,
                descriptions = listOf("Result from Manul"),
                scheduler = null
            ).first { it !is Content.Loading }

            val createdTask = (result as Content.Data).data.single()
            assertEquals(2, createdTask.id)
            assertEquals(1, createdTask.parentTaskId)
            assertEquals("Result from Manul", createdTask.description)

            val refreshedTasks = repository.observeTasks().first { it is Content.Data } as Content.Data
            val root = refreshedTasks.data.single()
            assertEquals(1, root.id)
            assertEquals(listOf(createdTask.id), root.subTasks.map { it.id })
            assertEquals(1, root.subTasks.single().parentTaskId)
            assertEquals("Result from Manul", root.subTasks.single().description)
        } finally {
            client.close()
        }
    }

    @Test
    fun weeklySchedulerDoesNotRunOnAnUnselectedWeekday() = runTest {
        val today = currentTime()
        val todayDate = today.date.toString()
        val unselectedWeekday = today.dayOfWeek.isoDayNumber % 7 + 1
        val source = LocalDataSource(InMemoryFileCache())
        source.init(listOf(weeklyTask(todayDate, unselectedWeekday)))

        val tasks = source.getAll()

        assertEquals(1, tasks.size)
        assertNull(tasks.single().scheduler?.lastDate)
    }
}

private fun rootTask() = TaskDto(
    id = 1,
    description = "Root task",
    addedTime = "2026-10-30T10:00:00",
    modifiedTime = "2026-10-30T10:00:00",
    parentTaskId = null,
    subTasks = emptyList(),
    isToDo = true,
    priority = 1
)

private fun weeklyTask(todayDate: String, weekday: Int) = TaskDto(
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
