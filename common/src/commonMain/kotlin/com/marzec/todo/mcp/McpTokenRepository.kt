package com.marzec.todo.mcp

import com.marzec.content.Content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

interface McpTokenRepository {
    suspend fun getOrCreateToken(): String
    suspend fun saveToken(token: String)
    suspend fun regenerateToken(): String
}

internal fun McpTokenRepository.getOrCreateTokenContent(): Flow<Content<String>> =
    tokenContentFlow { getOrCreateToken() }

internal fun McpTokenRepository.saveTokenContent(token: String): Flow<Content<String>> =
    tokenContentFlow {
        saveToken(token)
        token
    }

internal fun McpTokenRepository.regenerateTokenContent(): Flow<Content<String>> =
    tokenContentFlow { regenerateToken() }

private fun <T> tokenContentFlow(request: suspend () -> T): Flow<Content<T>> =
    flow {
        emit(Content.Loading())
        emit(Content.Data(request()))
    }.catch { exception ->
        when (exception) {
            is CancellationException -> throw exception
            is Exception -> emit(Content.Error(exception))
            else -> throw exception
        }
    }

internal fun isValidLocalMcpToken(token: String): Boolean =
    Regex("^[A-Za-z0-9._~-]{32,256}$").matches(token)
