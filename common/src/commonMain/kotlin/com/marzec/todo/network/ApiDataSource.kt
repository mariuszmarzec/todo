package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.api.MarkAsToDoDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.marzec.todo.Api.Todo.SSE as SSE_PATH

class ApiDataSource(
    private val client: HttpClient,
    private val sseClient: HttpClient,
    private val commonDataSource: CommonTodoDataSource
) : DataSource, CommonTodoDataSource by commonDataSource {

    override suspend fun sse(): Flow<ServerSentEvent> {
        return flow {
            sseClient.sse(
                urlString = SSE_PATH,
            ) {
                incoming.collect {
                    this@flow.emit(it)
                }
            }
        }
    }

    override suspend fun removeTask(taskId: Int, removeSubtasks: Boolean) {
        client.delete(Api.Todo.removeTaskWithSubtask(taskId, removeSubtasks))
    }

    override suspend fun copyTask(taskId: Int) {
        client.get(Api.Todo.copyTask(taskId))
    }

    override suspend fun markAsToDo(request: MarkAsToDoDto) {
        client.post(Api.Todo.MARK_AS_TO_DO) {
            setBody(request)
        }
    }
}
