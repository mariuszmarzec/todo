package com.marzec.todo.repository

import com.marzec.todo.Api
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.cache.Cache
import com.marzec.todo.extensions.flatMapTask
import com.marzec.todo.model.Task
import com.marzec.todo.model.toDomain
import com.marzec.todo.network.Content
import com.marzec.todo.network.DataSource
import com.marzec.todo.network.asContent
import com.marzec.todo.network.asContentFlow
import com.marzec.todo.network.ifDataSuspend
import com.marzec.todo.network.mapData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

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


    suspend fun pinAllTask(
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
    ): Flow<Content<T>> =
        withContext(dispatcher) {
            val cached = memoryCache.observe<T>(key).firstOrNull()
            val initial: Content<T> = if (cached != null) {
                Content.Data(cached)
            } else {
                Content.Loading()
            }
            merge(
                flow {
                    emit(initial)
                    val callResult = networkCall()
                    if (callResult is Content.Error) {
                        emit(callResult)
                    } else if (callResult is Content.Data) {
                        memoryCache.put(key, callResult.data)
                    }
                },
                memoryCache.observe<T>(key).filterNotNull().map { Content.Data(it) as Content<T> }
            )
        }.flowOn(dispatcher)

    private fun asContentWithListUpdate(request: suspend () -> Unit) =
        asContentFlow(request)
            .onEach {
                if (it is Content.Data) {
                    refreshListsCache()
                }
            }.flowOn(dispatcher)
}
