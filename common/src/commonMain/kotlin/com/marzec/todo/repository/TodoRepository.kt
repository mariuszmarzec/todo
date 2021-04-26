package com.marzec.todo.repository

import com.marzec.todo.Api
import com.marzec.todo.DI
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.CreateTodoListDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.todo.api.toDomain
import com.marzec.todo.model.Task
import com.marzec.todo.model.ToDoList
import com.marzec.todo.model.UpdateTaskDto
import com.marzec.todo.network.Content
import com.marzec.todo.network.asContent
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.withContext

class TodoRepository(private val client: HttpClient) {

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
                    contentType(ContentType.Application.Json)
                    body = CreateTodoListDto(title = title)
                }
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
                }.tasks.first {
                    it.id == taskId
                }
            }
        }

    suspend fun addNewTask(listId: Int, description: String): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.post(Api.Todo.createTask(listId)) {
                    contentType(ContentType.Application.Json)
                    body = CreateTaskDto(
                        description = description,
                        parentTaskId = null,
                        priority = 10
                    )
                }
            }
        }

    suspend fun updateTask(taskId: Int, description: String): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.patch(Api.Todo.updateTask(taskId)) {
                    contentType(ContentType.Application.Json)
                    body = UpdateTaskDto(
                        description = description,
                        parentTaskId = null,
                        priority = 10,
                        isToDo = true
                    )
                }
            }
        }

    suspend fun removeTask(taskId: Int): Content<Unit> =
        withContext(DI.ioDispatcher) {
            asContent {
                client.delete(Api.Todo.updateTask(taskId))
            }
        }
}