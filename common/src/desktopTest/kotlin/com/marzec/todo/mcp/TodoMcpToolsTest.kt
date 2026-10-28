package com.marzec.todo.mcp

import com.marzec.content.Content
import com.marzec.todo.model.Task
import com.marzec.todo.repository.TodoRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodoMcpToolsTest {

    @Test
    fun registersRepositoryToolsAndListsTasksThroughMcp() = runBlocking {
        val repository = io.mockk.mockk<TodoRepository>()
        coEvery { repository.observeTasks() } returns flowOf(Content.Data(emptyList()))

        withConnectedClient(repository) { client ->
            val names = client.listTools().tools.map { it.name }.toSet()
            assertContains(names, "list_tasks")
            assertContains(names, "list_scheduled_tasks")
            assertContains(names, "get_task")
            assertContains(names, "create_task")
            assertContains(names, "create_tasks")
            assertContains(names, "create_task_tree")
            assertContains(names, "update_task")
            assertContains(names, "mark_tasks_done")
            assertContains(names, "mark_tasks_to_do")
            assertContains(names, "reorder_tasks")
            assertContains(names, "schedule_tasks")
            assertContains(names, "copy_task")
            assertContains(names, "remove_task")
            assertContains(names, "leave_shared_task")
            assertContains(names, "list_shareable_users")

            val result = client.callTool("list_tasks", emptyMap())
            assertFalse(result.isError == true)
            assertEquals("[]", (result.content.first() as TextContent).text)
            coVerify(exactly = 1) { repository.observeTasks() }
        }
    }


    @Test
    fun explicitSharingOperationFailsInLocalStorageMode() = runBlocking {
        val repository = io.mockk.mockk<TodoRepository>()

        withConnectedClient(repository) { client ->
            val result = client.callTool(
                "create_task",
                mapOf(
                    "description" to "Do not silently ignore shares",
                    "shares" to emptyList<Any>()
                )
            )
            assertTrue(result.isError == true)
            assertContains(
                (result.content.first() as TextContent).text,
                "not supported in local-storage/mock mode"
            )
        }
    }

    @Test
    fun repositoryErrorsBecomeMcpToolErrors() = runBlocking {
        val repository = io.mockk.mockk<TodoRepository>()
        coEvery { repository.observeTasks() } returns flowOf(
            Content.Error<List<Task>>(IllegalStateException("data source unavailable"))
        )

        withConnectedClient(repository) { client ->
            val result = client.callTool("list_tasks", emptyMap())
            assertTrue(result.isError == true)
            assertContains((result.content.first() as TextContent).text, "data source unavailable")
        }
    }

    @Test
    fun createTaskToolInvokesRepositoryWithValidatedArguments() = runBlocking {
        val repository = io.mockk.mockk<TodoRepository>()
        every {
            repository.addNewTask(
                description = "Create from MCP",
                parentTaskId = null,
                highestPriorityAsDefault = false,
                scheduler = null,
                expirationDate = null,
                shares = null
            )
        } returns flowOf(Content.Data(Unit))

        withConnectedClient(repository) { client ->
            val result = client.callTool(
                "create_task",
                mapOf("description" to "Create from MCP")
            )
            assertFalse(result.isError == true)
            assertContains((result.content.first() as TextContent).text, "\"success\":true")
            verify(exactly = 1) {
                repository.addNewTask(
                    description = "Create from MCP",
                    parentTaskId = null,
                    highestPriorityAsDefault = false,
                    scheduler = null,
                    expirationDate = null,
                    shares = null
                )
            }
        }
    }

    @Test
    fun invalidTaskIdReturnsProtocolToolErrorWithoutRepositoryCall() = runBlocking {
        val repository = io.mockk.mockk<TodoRepository>()

        withConnectedClient(repository) { client ->
            val result = client.callTool("get_task", mapOf("taskId" to 0))
            assertTrue(result.isError == true)
            assertContains((result.content.first() as TextContent).text, "positive integer")
            coVerify(exactly = 0) { repository.observeTask(any()) }
        }
    }

    private suspend fun withConnectedClient(
        repository: TodoRepository,
        action: suspend (Client) -> Unit
    ) {
        val port = ServerSocket(0).use { it.localPort }
        val server = TodoMcpServer(port, repository)
        val httpClient = HttpClient(CIO) {
            install(SSE)
        }
        val client = Client(
            clientInfo = Implementation(
                name = "todo-mcp-tools-test",
                version = "1.0.0"
            )
        )
        var serverStarted = false
        var clientConnected = false

        try {
            server.start()
            serverStarted = true
            client.connect(
                StreamableHttpClientTransport(
                    client = httpClient,
                    url = "http://127.0.0.1:$port/mcp"
                )
            )
            clientConnected = true
            withTimeout(30_000) { action(client) }
        } finally {
            try {
                if (clientConnected) {
                    client.close()
                }
            } finally {
                try {
                    httpClient.close()
                } finally {
                    if (serverStarted) {
                        server.stop()
                    }
                }
            }
        }
    }
}
