package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.Api.Todo.ADD_TASKS
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.MarkAsToDoDto
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

    override suspend fun getTasks() = client.get<List<TaskDto>>(Api.Todo.TASKS)

    override suspend fun copyTask(taskId: Int) = client.get<Unit>(Api.Todo.copyTask(taskId))

    override suspend fun markAsToDo(request: MarkAsToDoDto) {
        client.post<Unit>(Api.Todo.MARK_AS_TO_DO) {
            body = request
        }
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
