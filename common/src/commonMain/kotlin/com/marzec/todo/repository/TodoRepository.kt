package com.marzec.todo.repository

import com.marzec.todo.Api
import com.marzec.todo.DI
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.toDomain
import com.marzec.todo.cache.Cache
import com.marzec.todo.model.Task
import com.marzec.todo.model.ToDoList
import com.marzec.todo.network.Content
import com.marzec.todo.network.ApiDataSource
import com.marzec.todo.network.DataSource
import com.marzec.todo.network.asContent
import com.marzec.todo.network.asContentFlow
import com.marzec.todo.network.ifDataSuspend
import com.marzec.todo.network.mapData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class TodoRepository(
    private val dataSource: DataSource,
    private val memoryCache: Cache
) {

    suspend fun observeLists(): Flow<Content<List<ToDoList>>> = getListsCacheFirst()

    suspend fun observeList(listId: Int): Flow<Content<ToDoList>> =
        getListsCacheFirst().map { content ->
            content.mapData { lists -> lists.first { it.id == listId } }
        }

    suspend fun observeTask(listId: Int, taskId: Int): Flow<Content<Task>> =
        getListsCacheFirst().map { content ->
            content.mapData { lists ->
                lists.first {
                    it.id == listId
                }.tasks.flatMapTask().first {
                    it.id == taskId
                }
            }
        }

    suspend fun addNewTask(
        listId: Int,
        description: String,
        parentTaskId: Int? = null,
        highestPriorityAsDefault: Boolean
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.addNewTask(
                listId, CreateTaskDto(
                    description = description,
                    parentTaskId = parentTaskId,
                    highestPriorityAsDefault = highestPriorityAsDefault
                )
            )
        }

    suspend fun addNewTasks(
        listId: Int,
        highestPriorityAsDefault: Boolean,
        parentTaskId: Int? = null,
        descriptions: List<String>
    ): Flow<Content<Unit>> =
        asContentWithListUpdate {
            descriptions.forEach {
                dataSource.addNewTask(
                    listId, CreateTaskDto(
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

    suspend fun removeTask(taskId: Int): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.removeTask(taskId)
        }

    fun removeList(id: Int): Flow<Content<Unit>> =
        asContentWithListUpdate { dataSource.removeList(id) }

    suspend fun createList(title: String): Flow<Content<Unit>> =
        asContentWithListUpdate {
            dataSource.createToDoList(title)
        }

    private suspend fun getListsCacheFirst() =
        cacheCall(Api.Todo.TODO_LISTS) {
            asContent {
                dataSource.getTodoLists()
                    .map { it.toDomain() }
            }
        }

    private suspend fun refreshListsCache() = asContent {
        dataSource.getTodoLists().map { it.toDomain() }
    }.ifDataSuspend {
        memoryCache.put(Api.Todo.TODO_LISTS, data)
    }

    private suspend fun <T : Any> cacheCall(
        key: String,
        networkCall: suspend () -> Content<T>
    ): Flow<Content<T>> =
        withContext(DI.ioDispatcher) {
            val cached = memoryCache.observe<T>(key).firstOrNull()
            val initial = if (cached != null) {
                Content.Data(cached)
            } else {
                Content.Loading()
            }
            combine(
                flow {
                    emit(initial)
                    val callResult = networkCall()
                    if (callResult is Content.Error) {
                        emit(callResult)
                    } else if (callResult is Content.Data) {
                        memoryCache.put(key, callResult.data)
                    }
                },
                memoryCache.observe<T>(key)
            ) { networkCall, cache ->
                if (cache != null) {
                    Content.Data(cache)
                } else {
                    networkCall
                }
            }.flowOn(DI.ioDispatcher)
        }

    private fun asContentWithListUpdate(request: suspend () -> Unit) =
        asContentFlow(request)
            .onEach {
                if (it is Content.Data) {
                    refreshListsCache()
                }
            }
}

private fun List<Task>.flatMapTask(tasks: MutableList<Task> = mutableListOf()): List<Task> {
    forEach {
        tasks.add(it)
        it.subTasks.flatMapTask(tasks)
    }
    return tasks
}
