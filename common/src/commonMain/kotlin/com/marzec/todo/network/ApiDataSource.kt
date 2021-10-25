package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.CreateTodoListDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.todo.api.UpdateTaskDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post

class ApiDataSource(
    private val client: HttpClient
) : DataSource {
    override suspend fun removeTask(taskId: Int) {
        client.delete<Unit>(Api.Todo.removeTask(taskId))
    }

    override suspend fun getTodoLists() = client.get<List<ToDoListDto>>(Api.Todo.TODO_LISTS)

    override suspend fun removeList(id: Int): Unit =
        client.delete(Api.Todo.removeList(id))

    override suspend fun createToDoList(title: String) =
        client.post<Unit>(Api.Todo.TODO_LIST) {
            body = CreateTodoListDto(title = title)
        }

    override suspend fun addNewTask(listId: Int, createTaskDto: CreateTaskDto) {
        client.post<Unit>(Api.Todo.createTask(listId)) {
            body = createTaskDto
        }
    }

    override suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean
    ) = client.patch<Unit>(Api.Todo.updateTask(taskId)) {
        body = UpdateTaskDto(
            description = description,
            parentTaskId = parentTaskId,
            priority = priority,
            isToDo = isToDo
        )
    }
}
