package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.Api.Todo.ADD_TASKS
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.SchedulerDto
import com.marzec.todo.api.TaskDto
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

    override suspend fun getTasks() = client.get<List<TaskDto>>(Api.Todo.TASKS).apply {
        println(this)
    }

    override suspend fun addNewTask(createTaskDto: CreateTaskDto) {
        client.post<Unit>(ADD_TASKS) {
            body = createTaskDto
        }
    }

    override suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean,
        scheduler: SchedulerDto?
    ) = client.patch<Unit>(Api.Todo.updateTask(taskId)) {
        body = UpdateTaskDto(
            description = description,
            parentTaskId = parentTaskId,
            priority = priority,
            isToDo = isToDo,
            scheduler = scheduler
        )
    }
}
