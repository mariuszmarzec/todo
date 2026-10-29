package com.marzec.todo.mcp

interface McpTokenStore {
    suspend fun getOrCreateToken(): String
    suspend fun saveToken(token: String)
    suspend fun regenerateToken(): String
}

internal fun isValidLocalMcpToken(token: String): Boolean =
    Regex("^[A-Za-z0-9._~-]{32,256}$").matches(token)
