package com.marzec.todo.repository

import com.marzec.cache.Cache
import com.marzec.cache.asContentWithListUpdate
import com.marzec.cache.cacheCall
import com.marzec.content.Content
import com.marzec.content.asContent
import com.marzec.content.ifDataSuspend
import com.marzec.content.mapData
import com.marzec.dto.NullableFieldDto
import com.marzec.model.toDto
import com.marzec.model.toNullableUpdate
import com.marzec.repository.CrudRepository
import com.marzec.todo.Api
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.MarkAsToDoDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.UpdateTaskDto
import com.marzec.todo.extensions.flatMapTask
import com.marzec.todo.model.CreateTask
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task
import com.marzec.todo.model.UpdateTask
import com.marzec.todo.model.toDomain
import com.marzec.todo.model.toDto
import com.marzec.todo.network.DataSource
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

typealias TodoCrudRepository = CrudRepository<Int, Task, CreateTask, UpdateTask, TaskDto, CreateTaskDto, UpdateTaskDto>

class TodoRepository(
    private val dataSource: DataSource,
    private val memoryCache: Cache,
    private val dispatcher: CoroutineDispatcher
) {

    suspend fun observeTasks(): Flow<Content<List<Task>>> = getTasksCacheFirst().map { content ->
        content.mapData { tasks ->
            tasks.filter { it.scheduler == null }
        }
    }

    suspend fun observeScheduledTasks(): Flow<Content<List<Task>>> =
        getTasksCacheFirst().map { content ->
            content.mapData { tasks ->
                tasks.filter { it.scheduler != null }
            }
        }

    suspend fun observeTask(taskId: Int): Flow<Content<Task>> =
        getTasksCacheFirst().map { content ->
            content.mapData { tasks ->
                tasks.flatMapTask().first {
                    it.id == taskId
                }
            }
        }

    suspend fun addNewTask(
        description: String,
        parentTaskId: Int?,
        highestPriorityAsDefault: Boolean,
        scheduler: Scheduler?
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.create(
                CreateTaskDto(
                    description = description,
                    parentTaskId = parentTaskId,
                    highestPriorityAsDefault = highestPriorityAsDefault,
                    scheduler = scheduler?.toDto()
                )
            )
        }

    suspend fun addNewTasks(
        highestPriorityAsDefault: Boolean,
        parentTaskId: Int? = null,
        descriptions: List<String>,
        scheduler: Scheduler?
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            descriptions.forEach {
                dataSource.create(
                    CreateTaskDto(
                        description = it,
                        parentTaskId = parentTaskId,
                        highestPriorityAsDefault = highestPriorityAsDefault,
                        scheduler = scheduler?.toDto()
                    )
                )
            }
        }

    suspend fun updateTask(
        taskId: Int,
        task: UpdateTask
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.update(taskId, task.toDto())
        }

    suspend fun markAsDone(taskId: Int): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.markAsToDo(
                MarkAsToDoDto(
                    isToDo = false,
                    taskIds = listOf(taskId)
                )
            )
        }


    suspend fun markAsDone(taskIds: List<Int>): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.markAsToDo(
                MarkAsToDoDto(
                    isToDo = false,
                    taskIds = taskIds
                )
            )
        }

    suspend fun markAsToDo(taskId: Int): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.markAsToDo(
                MarkAsToDoDto(
                    isToDo = true,
                    taskIds = listOf(taskId)
                )
            )
        }


    suspend fun markAsToDo(taskIds: List<Int>): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.markAsToDo(
                MarkAsToDoDto(
                    isToDo = true,
                    taskIds = taskIds
                )
            )
        }

    suspend fun pinTask(
        task: Task,
        parentTaskId: Int?
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.update(
                id = task.id,
                UpdateTaskDto(
                    parentTaskId = parentTaskId.toNullableUpdate(task.parentTaskId)?.toDto()
                )
            )
        }

    suspend fun pinAllTasks(
        tasks: List<Task>,
        parentTaskId: Int?
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            tasks.forEach { task ->
                dataSource.update(
                    id = task.id,
                    UpdateTaskDto(
                        parentTaskId = parentTaskId.toNullableUpdate(task.parentTaskId)?.toDto()
                    )
                )
            }
        }

    suspend fun reorderByPriority(
        tasks: List<Task>
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            tasks.reversed().forEachIndexed { index, task ->
                dataSource.update(
                    id = task.id,
                    UpdateTaskDto(
                        priority = index
                    )
                )
            }
        }

    suspend fun removeTask(taskId: Int): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.removeTask(taskId)
        }

    suspend fun removeTasks(taskIds: List<Int>): Flow<Content<Unit>> =
        asContentWithListUpdate {
            taskIds.forEach {
                dataSource.removeTask(it)
            }
        }

    fun removeTaskWithSubtasks(task: Task): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.removeTask(taskId = task.id, removeSubtasks = true)
        }

    fun removeTasksWithSubtasks(tasks: List<Task>): Flow<Content<Unit>> =
        asContentWithListUpdate {
            tasks.forEach { task ->
                dataSource.removeTask(taskId = task.id, removeSubtasks = true)
            }
        }

    fun copyTask(taskId: Int): Flow<Content<Unit>> = asContentWithListUpdate {
        dataSource.copyTask(taskId)
    }

    private suspend fun getTasksCacheFirst() =
        cacheCall(Api.Todo.TASKS) {
            asContent {
                dataSource.getAll()
                    .map { it.toDomain() }
            }
        }

    private suspend fun refreshListsCache() = asContent {
        dataSource.getAll().map { it.toDomain() }
    }.ifDataSuspend {
        memoryCache.put(Api.Todo.TASKS, data)
    }

    private suspend fun <T : Any> cacheCall(
        key: String,
        networkCall: suspend () -> Content<T>
    ): Flow<Content<T>> = cacheCall(key, dispatcher, memoryCache, networkCall)

    suspend fun schedule(tasks: List<Task>, scheduler: Scheduler): Flow<Content<Unit>> =
        asContentWithListUpdate {
            val scope = CoroutineScope(coroutineContext + dispatcher + SupervisorJob())
            tasks.map {
                scope.async {
                    dataSource.update(
                        id = it.id,
                        update = UpdateTaskDto(scheduler = NullableFieldDto(scheduler.toDto()))
                    )
                }
            }.map { it.await() }
        }

    private fun asContentWithListUpdate(
        request: suspend () -> Unit
    ) = asContentWithListUpdate(dispatcher, ::refreshListsCache, request)
}
