package com.marzec.todo.repository

import com.marzec.todo.Api
import com.marzec.todo.DI
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.CreateTodoListDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.todo.api.UpdateTaskDto
import com.marzec.todo.api.toDomain
import com.marzec.todo.cache.MemoryCache
import com.marzec.todo.model.Task
import com.marzec.todo.model.ToDoList
import com.marzec.todo.network.Content
import com.marzec.todo.network.DataSource
import com.marzec.todo.network.asContent
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class TodoRepository(
    private val dataSource: DataSource,
    private val memoryCache: MemoryCache
) {

    suspend fun getLists(): Content<List<ToDoList>> =
        withContext(DI.ioDispatcher) {
            asContent { dataSource.getTodoLists().map { it.toDomain() } }
        }

    suspend fun observeLists(listId: Int): Flow<Content<ToDoList>> =
        cacheCall(Api.Todo.TODO_LISTS + "_" + "$listId") {
            asContent {
                dataSource.getTodoLists()
                    .map { it.toDomain() }
                    .first { it.id == listId }
            }
        }

    suspend fun getList(listId: Int): Content<ToDoList> =
        withContext(DI.ioDispatcher) {
            asContent {
                dataSource.getTodoLists().map { it.toDomain() }.first {
                    it.id == listId
                }
            }
        }

    suspend fun getTask(listId: Int, taskId: Int): Content<Task> =
        withContext(DI.ioDispatcher) {
            asContent {
                dataSource.getTodoLists().map { it.toDomain() }.first {
                    it.id == listId
                }.tasks.flatMapTask().first {
                    it.id == taskId
                }
            }
        }

    suspend fun addNewTask(
        listId: Int,
        parentTaskId: Int? = null,
        description: String
    ): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent { dataSource.addNewTask(listId, description, parentTaskId) }
        }

    suspend fun addNewTasks(
        listId: Int,
        parentTaskId: Int? = null,
        descriptions: List<String>
    ): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent {
                descriptions.forEach {
                    dataSource.addNewTask(listId, it, parentTaskId)
                }
            }
        }

    suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean
    ): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent {
                dataSource.updateTask(taskId, description, parentTaskId, priority, isToDo)
            }
        }

    suspend fun removeTask(taskId: Int): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent {
                dataSource.removeTask(taskId)
            }
        }

    suspend fun removeList(id: Int): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent { dataSource.removeList(id) }
        }

    suspend fun createList(title: String): Content<List<ToDoListDto>> =
        withContext(DI.ioDispatcher) {
            asContent { dataSource.createToDoList(title) }
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
