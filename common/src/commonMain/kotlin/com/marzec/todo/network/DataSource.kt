package com.marzec.todo.network

import com.marzec.datasource.CrudDataSource
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.MarkAsToDoDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.UpdateTaskDto
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.Flow

interface DataSource : CommonTodoDataSource {

    suspend fun removeTask(taskId: Int, removeSubtasks: Boolean = false)

    suspend fun copyTask(taskId: Int)

    suspend fun markAsToDo(request: MarkAsToDoDto)

    suspend fun sse(): Flow<ServerSentEvent>
}

typealias CommonTodoDataSource = CrudDataSource<Int, TaskDto, CreateTaskDto, UpdateTaskDto>