package com.marzec.todo.repository

import com.marzec.todo.Api
import com.marzec.todo.DI
import com.marzec.todo.api.CreateTodoListDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.todo.api.toDomain
import com.marzec.todo.model.ToDoList
import com.marzec.todo.network.Content
import com.marzec.todo.network.asContent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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

}