package com.marzec.todo.mcp

import com.marzec.common.CopyToClipBoardHelper
import com.marzec.content.Content
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceWithResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class LocalMcpTokenSettingsStore(
    scope: CoroutineScope,
    private val store: McpTokenRepository,
    private val copyToClipBoardHelper: CopyToClipBoardHelper
) : Store4Impl<State<LocalMcpTokenSettingsState>>(
    scope, LocalMcpTokenSettingsState.INITIAL
) {

    fun loadToken() = intent<Content<String>>("loadToken") {
        onTrigger { store.getOrCreateTokenContent() }

        reducer {
            state.reduceWithResult(resultNonNull()) { result ->
                copy(
                    currentToken = result.data,
                    draftToken = result.data,
                    error = null,
                    status = null
                )
            }
        }
    }

    fun onDraftTokenChanged(text: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(draftToken = text, error = null, status = null) }
        }
    }

    fun saveToken() = intent<Content<String>>("saveToken") {
        onTrigger {
            val draft = state.data?.draftToken.orEmpty()
            if (isValidLocalMcpToken(draft)) {
                store.saveTokenContent(draft)
            } else {
                flowOf(
                    Content.Error<String>(
                        IllegalArgumentException(
                            "Token must be 32-256 URL-safe characters using letters, " +
                                "digits, dot, underscore, tilde or hyphen"
                        )
                    )
                )
            }
        }

        reducer {
            when (val result = resultNonNull()) {
                is Content.Loading -> state.reduceData {
                    copy(saving = true, error = null, status = null)
                }

                is Content.Data -> state.reduceData {
                    copy(
                        saving = false,
                        currentToken = result.data,
                        draftToken = result.data,
                        status = "Token updated. The previous stored token has been replaced.",
                        error = null
                    )
                }

                is Content.Error -> state.reduceData {
                    copy(
                        saving = false,
                        error = result.exception.message ?: "Could not update the local MCP token"
                    )
                }

                else -> state
            }
        }
    }

    fun regenerateToken() = intent<Content<String>>("regenerateToken") {
        onTrigger { store.regenerateTokenContent() }

        reducer {
            when (val result = resultNonNull()) {
                is Content.Loading -> state.reduceData {
                    copy(saving = true, error = null, status = null)
                }

                is Content.Data -> state.reduceData {
                    copy(
                        saving = false,
                        currentToken = result.data,
                        draftToken = result.data,
                        status = "Token updated. The previous stored token has been replaced.",
                        error = null
                    )
                }

                is Content.Error -> state.reduceData {
                    copy(
                        saving = false,
                        error = result.exception.message ?: "Could not update the local MCP token"
                    )
                }

                else -> state
            }
        }
    }

    fun copyToken() = intent<Unit> {
        onTrigger { flowOf(Unit) }

        sideEffect {
            state.ifDataAvailable {
                copyToClipBoardHelper.copy(currentToken)
            }
        }

        reducer {
            state.reduceData {
                if (currentToken.isNotEmpty()) {
                    copy(status = "Token copied to clipboard.")
                } else {
                    this
                }
            }
        }
    }
}