package com.marzec.todo.repository

import com.marzec.cache.FileCache
import com.marzec.cache.MemoryCache
import com.marzec.content.Content
import com.marzec.logger.Logger
import com.marzec.logger.MultiLogger
import com.marzec.repository.LoginRepositoryMock
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.TaskShareDto
import com.marzec.todo.network.LocalDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertTrue

/**
 * Covers owner-based task removal through [TodoRepository].
 * A null ownerId means the task belongs to the current user, so removal must
 * use the normal remove path rather than falling through to leaveShare.
 */
class TodoRepositoryTest {

    private suspend fun newRepository(
        fileCache: InMemoryFileCache,
        dataSource: LocalDataSource,
    ): TodoRepository {
        Logger.logger = MultiLogger(emptyList())
        val repository = TodoRepository(
            dataSource = dataSource,
            memoryCache = MemoryCache(),
            fileCache = fileCache,
            dispatcher = Dispatchers.Unconfined,
            loginRepository = LoginRepositoryMock()
        )
        // Warm the tasks cache the same way the task list screen does before
        // any removal is attempted; removeTask resolves the task from the cache.
        repository.observeTasks().first { it is Content.Data }
        return repository
    }

    @Test
    fun removeTaskTreatsTaskWithoutOwnerAsOwnedByCurrentUser() = runTest {
        val fileCache = InMemoryFileCache()
        val dataSource = LocalDataSource(fileCache)
        dataSource.init(listOf(localTask(id = 1, ownerId = null)))
        val repository = newRepository(fileCache, dataSource)

        val result = repository.removeTask(1).first { it !is Content.Loading }

        assertTrue(result is Content.Data, "Expected success but got $result")
        assertEquals(emptyList(), dataSource.getAll())
    }

    @Test
    fun removeTasksDeletesMultipleLocallyCreatedTasks() = runTest {
        val fileCache = InMemoryFileCache()
        val dataSource = LocalDataSource(fileCache)
        dataSource.init(listOf(localTask(id = 1), localTask(id = 2)))
        val repository = newRepository(fileCache, dataSource)

        val result = repository.removeTasks(listOf(1, 2)).first { it !is Content.Loading }

        assertTrue(result is Content.Data, "Expected success but got $result")
        assertEquals(emptyList(), dataSource.getAll())
    }

    @Test
    fun removeTaskWithSubtasksDeletesLocalTaskTree() = runTest {
        val fileCache = InMemoryFileCache()
        val dataSource = LocalDataSource(fileCache)
        dataSource.init(
            listOf(
                localTask(id = 1),
                localTask(id = 2, parentTaskId = 1),
                localTask(id = 3, parentTaskId = 2),
            )
        )
        val repository = newRepository(fileCache, dataSource)
        val root = (repository.observeTask(1).first { it is Content.Data } as Content.Data).data

        val result = repository.removeTaskWithSubtasks(root).first { it !is Content.Loading }

        assertTrue(result is Content.Data, "Expected success but got $result")
        assertEquals(emptyList(), dataSource.getAll())
    }

    @Test
    fun removeTaskOnTaskOwnedByCurrentUserSucceeds() = runTest {
        val fileCache = InMemoryFileCache()
        val dataSource = LocalDataSource(fileCache)
        // LoginRepositoryMock returns user id = 1.
        dataSource.init(listOf(localTask(id = 1, ownerId = 1)))
        val repository = newRepository(fileCache, dataSource)

        val result = repository.removeTask(1).first { it !is Content.Loading }

        assertTrue(result is Content.Data, "Expected success but got $result")
        assertEquals(emptyList(), dataSource.getAll())
    }

    @Test
    fun removeTaskOnForeignTaskWithEditorShareSucceeds() = runTest {
        val fileCache = InMemoryFileCache()
        val dataSource = LocalDataSource(fileCache)
        dataSource.init(
            listOf(
                localTask(
                    id = 1,
                    ownerId = 99,
                    shares = listOf(TaskShareDto(userId = "1", permission = "EDITOR_AND_VIEWER"))
                )
            )
        )
        val repository = newRepository(fileCache, dataSource)

        val result = repository.removeTask(1).first { it !is Content.Loading }

        assertTrue(result is Content.Data, "Expected success but got $result")
        assertEquals(emptyList(), dataSource.getAll())
    }

    @Test
    fun removeTaskOnForeignTaskWithoutShareFailsInsteadOfDeleting() = runTest {
        val fileCache = InMemoryFileCache()
        val dataSource = LocalDataSource(fileCache)
        // Foreign owner without any share for the current user: permission check
        // fails and the repository falls through to leaveShare, which the local
        // data source does not support. The task must remain untouched.
        dataSource.init(listOf(localTask(id = 1, ownerId = 99)))
        val repository = newRepository(fileCache, dataSource)

        val result = repository.removeTask(1).first { it !is Content.Loading }

        assertTrue(result is Content.Error, "Expected error but got $result")
        assertEquals(1, dataSource.getAll().size)
    }
}

private fun localTask(
    id: Int,
    parentTaskId: Int? = null,
    ownerId: Int? = null,
    shares: List<TaskShareDto> = emptyList(),
) = TaskDto(
    id = id,
    description = "Task $id",
    addedTime = "2026-11-03T10:00:00",
    modifiedTime = "2026-11-03T10:00:00",
    parentTaskId = parentTaskId,
    subTasks = emptyList(),
    isToDo = true,
    priority = 1,
    ownerId = ownerId,
    shares = shares,
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
