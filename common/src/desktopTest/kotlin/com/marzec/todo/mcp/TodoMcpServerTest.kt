package com.marzec.todo.mcp

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertTrue

class TodoMcpServerTest {

    @Test
    fun clientCanInitializeAndListToolsOverStreamableHttp() = runBlocking {
        // Reserve an available port for the test server. It is released before Ktor binds to it.
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
                    url = "http://127.0.0.1:$port/mcp"
                )
            )
            // connect() performs the MCP initialize handshake.
            clientConnected = true

            val tools = client.listTools().tools
            assertTrue(tools.any { it.name == "ping" }, "Expected the PoC ping tool to be discoverable")
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
