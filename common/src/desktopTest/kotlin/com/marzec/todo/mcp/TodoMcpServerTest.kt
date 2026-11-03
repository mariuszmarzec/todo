package com.marzec.todo.mcp

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TodoMcpServerTest {

    @Test
    fun clientCanInitializeAndListToolsOverUnauthenticatedStreamableHttp() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val server = TodoMcpServer(port)
        val httpClient = HttpClient(CIO) {
            install(SSE)
        }
        val client = Client(
            clientInfo = Implementation(
                name = "todo-mcp-integration-test",
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
                    url = "http://127.0.0.1:" + port + "/mcp"
                )
            )
            clientConnected = true

            val tools = client.listTools().tools
            assertTrue(tools.any { it.name == "ping" }, "Expected the ping tool to be discoverable")
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

    @Test
    fun unauthenticatedRequestSucceedsOnLocalMcp() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val server = TodoMcpServer(port)
        val httpClient = HttpClient(CIO)
        val url = "http://127.0.0.1:" + port + "/mcp"
        var serverStarted = false

        try {
            server.start()
            serverStarted = true

            assertNotEquals(
                HttpStatusCode.Unauthorized,
                httpClient.post(url).status
            )
        } finally {
            httpClient.close()
            if (serverStarted) {
                server.stop()
            }
        }
    }
}