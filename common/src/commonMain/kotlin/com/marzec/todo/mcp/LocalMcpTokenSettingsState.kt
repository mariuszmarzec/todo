package com.marzec.todo.mcp

import com.marzec.mvi.State

data class LocalMcpTokenSettingsState(
    val currentToken: String = "",
    val draftToken: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val status: String? = null
) {
    companion object {
        val INITIAL = State.Loading(LocalMcpTokenSettingsState(loading = true))
    }
}
