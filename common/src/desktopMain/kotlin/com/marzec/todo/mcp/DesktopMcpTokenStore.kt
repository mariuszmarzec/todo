package com.marzec.todo.mcp

import java.util.Base64
import java.util.prefs.Preferences
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopMcpTokenStore(
    private val preferences: Preferences = Preferences.userRoot().node("com/marzec/todo/local-mcp"),
    private val secureRandom: SecureRandom = SecureRandom()
) : McpTokenStore {

    override suspend fun getOrCreateToken(): String = withContext(Dispatchers.IO) {
        val current = preferences.get(TOKEN_KEY, null)
        if (current != null && isValidLocalMcpToken(current)) {
            current
        } else {
            generateAndStoreToken()
        }
    }

    override suspend fun saveToken(token: String) {
        require(isValidLocalMcpToken(token)) {
            "Token must be 32-256 URL-safe characters using letters, digits, dot, underscore, tilde or hyphen"
        }
        withContext(Dispatchers.IO) {
            preferences.put(TOKEN_KEY, token)
            preferences.flush()
        }
    }

    override suspend fun regenerateToken(): String = withContext(Dispatchers.IO) {
        generateAndStoreToken()
    }

    private fun generateAndStoreToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        preferences.put(TOKEN_KEY, token)
        preferences.flush()
        return token
    }

    companion object {
        private const val TOKEN_KEY = "localMcpToken"
        private const val TOKEN_BYTES = 32
    }
}
