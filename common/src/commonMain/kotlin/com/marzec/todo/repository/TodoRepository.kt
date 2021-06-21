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
    private val client: HttpClient,
    private val memoryCache: MemoryCache
) {

    suspend fun getLists(): Content<List<ToDoList>> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.get<List<ToDoListDto>>(Api.Todo.TODO_LISTS).map { it.toDomain() }
            }
        }

    suspend fun createList(title: String): Content<List<ToDoListDto>> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.post(Api.Todo.TODO_LIST) {
                    body = CreateTodoListDto(title = title)
                }
            }
        }

    suspend fun observeLists(listId: Int): Flow<Content<ToDoList>> =
        cacheCall(Api.Todo.TODO_LISTS + "_" + "$listId") {
            asContent {
                client.get<List<ToDoListDto>>(Api.Todo.TODO_LISTS)
                    .map { it.toDomain() }
                    .first { it.id == listId }
            }
        }

    suspend fun getList(listId: Int): Content<ToDoList> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.get<List<ToDoListDto>>(Api.Todo.TODO_LISTS).map { it.toDomain() }.first {
                    it.id == listId
                }
            }
        }

    suspend fun getTask(listId: Int, taskId: Int): Content<Task> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.get<List<ToDoListDto>>(Api.Todo.TODO_LISTS).map { it.toDomain() }.first {
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
            asContent {
                client.post(Api.Todo.createTask(listId)) {
                    body = CreateTaskDto(
                        description = description,
                        parentTaskId = parentTaskId,
                        priority = 10
                    )
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
                client.patch(Api.Todo.updateTask(taskId)) {
                    body = UpdateTaskDto(
                        description = description,
                        parentTaskId = parentTaskId,
                        priority = priority,
                        isToDo = isToDo
                    )
                }
            }
        }

    suspend fun removeTask(taskId: Int): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.delete(Api.Todo.removeTask(taskId))
            }
        }

    suspend fun removeList(id: Int): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.delete(Api.Todo.removeList(id))
            }
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
