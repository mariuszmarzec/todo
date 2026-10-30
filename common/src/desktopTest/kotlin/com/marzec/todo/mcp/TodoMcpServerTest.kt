package com.marzec.todo.mcp

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
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
    fun clientCanInitializeAndListToolsOverAuthenticatedStreamableHttp() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val tokenStore = TestMcpTokenRepository()
        val server = TodoMcpServer(port, tokenStore)
        val httpClient = HttpClient(CIO) {
            install(SSE)
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer " + tokenStore.currentToken)
            }
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
    fun rejectsMissingInvalidAndRotatedTokens() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val tokenStore = TestMcpTokenRepository()
        val originalToken = tokenStore.currentToken
        val server = TodoMcpServer(port, tokenStore)
        val unauthenticatedClient = HttpClient(CIO)
        val clientWithOriginalToken = HttpClient(CIO) {
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer " + originalToken)
            }
        }
        val url = "http://127.0.0.1:" + port + "/mcp"
        var serverStarted = false

        try {
            server.start()
            serverStarted = true

            assertEquals(HttpStatusCode.Unauthorized, unauthenticatedClient.post(url).status)
            assertEquals(
                HttpStatusCode.Unauthorized,
                unauthenticatedClient.post(url) {
                    header(HttpHeaders.Authorization, "Bearer invalid-token")
                }.status
            )
            assertNotEquals(HttpStatusCode.Unauthorized, clientWithOriginalToken.post(url).status)

            tokenStore.saveToken("rotated-local-mcp-token-012345678901234567890123")
            assertEquals(HttpStatusCode.Unauthorized, clientWithOriginalToken.post(url).status)
        } finally {
            unauthenticatedClient.close()
            clientWithOriginalToken.close()
            if (serverStarted) {
                server.stop()
            }
        }
    }
}
