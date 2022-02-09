package com.marzec.todo.repository

import com.marzec.cache.Cache
import com.marzec.cache.asContentWithListUpdate
import com.marzec.cache.cacheCall
import com.marzec.content.Content
import com.marzec.content.asContent
import com.marzec.content.ifDataSuspend
import com.marzec.content.mapData
import com.marzec.todo.Api
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.extensions.flatMapTask
import com.marzec.todo.model.Task
import com.marzec.todo.model.toDomain
import com.marzec.todo.network.DataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TodoRepository(
    private val dataSource: DataSource,
    private val memoryCache: Cache,
    private val dispatcher: CoroutineDispatcher
) {

    suspend fun observeLists(): Flow<Content<List<Task>>> = getTasksCacheFirst()

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
        parentTaskId: Int? = null,
        highestPriorityAsDefault: Boolean
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.addNewTask(
                CreateTaskDto(
                    description = description,
                    parentTaskId = parentTaskId,
                    highestPriorityAsDefault = highestPriorityAsDefault
                )
            )
        }

    suspend fun addNewTasks(
        highestPriorityAsDefault: Boolean,
        parentTaskId: Int? = null,
        descriptions: List<String>
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            descriptions.forEach {
                dataSource.addNewTask(
                    CreateTaskDto(
                        description = it,
                        parentTaskId = parentTaskId,
                        highestPriorityAsDefault = highestPriorityAsDefault
                    )
                )
            }
        }

    suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.updateTask(taskId, description, parentTaskId, priority, isToDo)
        }

    suspend fun markAsDone(task: Task): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.updateTask(
                taskId = task.id,
                description = task.description,
                parentTaskId = task.parentTaskId,
                priority = task.priority,
                isToDo = false
            )
        }


    suspend fun markAsDone(tasks: List<Task>): Flow<Content<Unit>> =
        asContentWithListUpdate {
            tasks.forEach { task ->
                dataSource.updateTask(
                    taskId = task.id,
                    description = task.description,
                    parentTaskId = task.parentTaskId,
                    priority = task.priority,
                    isToDo = false
                )
            }
        }

    suspend fun markAsToDo(task: Task): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.updateTask(
                taskId = task.id,
                description = task.description,
                parentTaskId = task.parentTaskId,
                priority = task.priority,
                isToDo = true
            )
        }


    suspend fun markAsToDo(tasks: List<Task>): Flow<Content<Unit>> =
        asContentWithListUpdate {
            tasks.forEach { task ->
                dataSource.updateTask(
                    taskId = task.id,
                    description = task.description,
                    parentTaskId = task.parentTaskId,
                    priority = task.priority,
                    isToDo = true
                )
            }
        }

    suspend fun pinTask(
        task: Task,
        parentTaskId: Int?
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.updateTask(
                taskId = task.id,
                description = task.description,
                parentTaskId = parentTaskId,
                priority = task.priority,
                isToDo = task.isToDo
            )
        }


    suspend fun pinAllTasks(
        tasks: List<Task>,
        parentTaskId: Int?
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            tasks.forEach { task ->
                dataSource.updateTask(
                    taskId = task.id,
                    description = task.description,
                    parentTaskId = parentTaskId,
                    priority = task.priority,
                    isToDo = task.isToDo
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
            task.subTasks.flatMapTask().forEach {
                dataSource.removeTask(it.id)
            }
            dataSource.removeTask(task.id)
        }

    fun removeTasksWithSubtasks(tasks: List<Task>): Flow<Content<Unit>> =
        asContentWithListUpdate {
            tasks.forEach { task ->
                task.subTasks.flatMapTask().forEach {
                    dataSource.removeTask(it.id)
                }
                dataSource.removeTask(task.id)
            }
        }

    private suspend fun getTasksCacheFirst() =
        cacheCall(Api.Todo.TASKS) {
            asContent {
                dataSource.getTasks()
                    .map { it.toDomain() }
            }
        }

    private suspend fun refreshListsCache() = asContent {
        dataSource.getTasks().map { it.toDomain() }
    }.ifDataSuspend {
        memoryCache.put(Api.Todo.TASKS, data)
    }

    private suspend fun <T : Any> cacheCall(
        key: String,
        networkCall: suspend () -> Content<T>
    ): Flow<Content<T>> = cacheCall(key, dispatcher, memoryCache, networkCall)

    private fun asContentWithListUpdate(
        request: suspend () -> Unit
    ) = asContentWithListUpdate(dispatcher, request) {
        refreshListsCache()
    }
}
