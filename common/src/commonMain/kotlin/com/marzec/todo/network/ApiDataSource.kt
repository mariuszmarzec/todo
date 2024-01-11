package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.Api.Todo.ADD_TASKS
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.MarkAsToDoDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.UpdateTaskDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse

class ApiDataSource(
    private val client: HttpClient
) : DataSource {
    override suspend fun removeTask(taskId: Int, removeSubtasks: Boolean) {
        client.delete(Api.Todo.removeTaskWithSubtask(taskId, removeSubtasks))
    }

    override suspend fun getTasks() = client.get(Api.Todo.TASKS).body<List<TaskDto>>()

    override suspend fun copyTask(taskId: Int) {
        client.get(Api.Todo.copyTask(taskId))
    }

    override suspend fun markAsToDo(request: MarkAsToDoDto) {
        client.post(Api.Todo.MARK_AS_TO_DO) {
            setBody(request)
        }
    }

    override suspend fun addNewTask(createTaskDto: CreateTaskDto) {
        client.post(ADD_TASKS) {
            setBody(createTaskDto)
        }
    }

    override suspend fun updateTask(
        taskId: Int,
        task: UpdateTaskDto
    ) {
        client.patch(Api.Todo.updateTask(taskId)) {
            setBody(task)
        }
    }
}
