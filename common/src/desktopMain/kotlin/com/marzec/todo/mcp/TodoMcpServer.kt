package com.marzec.todo.mcp

import com.marzec.todo.repository.TodoRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

interface LocalMcpServerInstance {
    fun start()
    fun stop()
}

class TodoMcpServer(
    private val port: Int,
    private val tokenStore: McpTokenStore,
    repository: TodoRepository? = null
) : LocalMcpServerInstance {
    init {
        require(port in 0..65535) { "Port must be between 0 and 65535" }
    }

    private val started = AtomicBoolean(false)

    private val mcpServer = Server(
        serverInfo = Implementation(
            name = "todo-desktop",
            version = "0.1.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false)
            )
        )
    ).apply {
        addTool(
            name = "ping",
            description = "Verify that the local Todo MCP endpoint is reachable."
        ) {
            CallToolResult(content = listOf(TextContent(text = "pong")))
        }
        repository?.let { TodoMcpTools(it).register(this) }
    }

    private val engine = embeddedServer(
        factory = CIO,
        host = LOOPBACK_HOST,
        port = port
    ) {
        intercept(ApplicationCallPipeline.Call) {
            if (call.request.path() == MCP_PATH) {
                val expectedToken = tokenStore.getOrCreateToken()
                val providedToken = parseBearerToken(call.request.headers[HttpHeaders.Authorization])
                if (providedToken == null || !tokensMatch(providedToken, expectedToken)) {
                    call.respond(HttpStatusCode.Unauthorized)
                    finish()
                }
            }
        }
        mcpStreamableHttp(
            path = MCP_PATH,
            enableDnsRebindingProtection = true
        ) {
            mcpServer
        }
    }

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        try {
            engine.start(wait = false)
        } catch (error: Exception) {
            started.set(false)
            throw error
        }
    }

    override fun stop() {
        if (started.compareAndSet(true, false)) {
            engine.stop(gracePeriodMillis = 500, timeoutMillis = 1_000)
        }
    }

    private fun parseBearerToken(header: String?): String? {
        val prefix = "Bearer "
        if (header == null || !header.startsWith(prefix, ignoreCase = true)) return null
        val token = header.substring(prefix.length)
        if (token.isBlank() || token.any(Char::isWhitespace)) return null
        return token
    }

    private fun tokensMatch(provided: String, expected: String): Boolean =
        MessageDigest.isEqual(
            provided.toByteArray(StandardCharsets.UTF_8),
            expected.toByteArray(StandardCharsets.UTF_8)
        )

    companion object {
        const val LOOPBACK_HOST = "127.0.0.1"
        const val MCP_PATH = "/mcp"
    }
}
