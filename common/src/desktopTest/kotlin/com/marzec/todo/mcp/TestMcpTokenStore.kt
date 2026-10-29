package com.marzec.todo.mcp

internal class TestMcpTokenStore(
    initialToken: String = "test-local-mcp-token-012345678901234567890123"
) : McpTokenStore {
    @Volatile
    var currentToken: String = initialToken
        private set

    override suspend fun getOrCreateToken(): String = currentToken

    override suspend fun saveToken(token: String) {
        currentToken = token
    }

    override suspend fun regenerateToken(): String {
        currentToken = "rotated-local-mcp-token-012345678901234567890123"
        return currentToken
    }
}
