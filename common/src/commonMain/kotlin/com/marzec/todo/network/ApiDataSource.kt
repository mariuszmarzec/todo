package com.marzec.todo.network

import com.marzec.logger.Logger
import com.marzec.todo.Api
import com.marzec.todo.api.LeaveShareDto
import com.marzec.todo.api.MarkAsToDoDto
import com.marzec.todo.api.TaskDto
import com.marzec.api.UserDto
import com.marzec.todo.DI
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.sse.ServerSentEvent
import kotlin.math.log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import com.marzec.todo.Api.Todo.SSE as SSE_PATH

class ApiDataSource(
    private val client: HttpClient,
    private val sseClient: HttpClient,
    private val commonDataSource: CommonTodoDataSource,
    private val logger: Logger,
) : DataSource, CommonTodoDataSource by commonDataSource {

    override suspend fun sse(): Flow<ServerSentEvent> = flow {
        var backoff = 1000L

        while (currentCoroutineContext().isActive) {
            try {
                sseClient.sse(urlString = SSE_PATH) {
                    incoming.collect { event ->
                        backoff = 1000L
                        emit(event)
                    }
                }
            } catch (e: Exception) {
                logger.log("SSE", "Disconnected: ${e.message}", e)

                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
        }
    }.flowOn(DI.ioDispatcher)

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

    override suspend fun leaveShare(leaveShareDto: LeaveShareDto): TaskDto {
        return client.post(Api.Todo.LEAVE_SHARE) {
            setBody(leaveShareDto)
        }.body<TaskDto>()
    }

    override suspend fun getUsers(): List<UserDto> {
        return client.get(Api.Todo.GET_USERS).body<List<UserDto>>()
    }
}
