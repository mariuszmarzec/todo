@file:Suppress("TooGenericExceptionCaught", "RethrowCaughtException")

package com.marzec.todo.mcp

import com.marzec.common.CopyToClipBoardHelper
import com.marzec.content.Content
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceWithResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class LocalMcpTokenSettingsStore(
    scope: CoroutineScope,
    private val store: McpTokenStore,
    private val copyToClipBoardHelper: CopyToClipBoardHelper
) : Store4Impl<State<LocalMcpTokenSettingsState>>(
    scope, LocalMcpTokenSettingsState.INITIAL
) {

    fun loadToken() = intent<Content<String>>("loadToken") {
        onTrigger {
            flow {
                try {
                    emit(Content.Data(store.getOrCreateToken()))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    emit(Content.Error(exception))
                }
            }
        }

        reducer {
            state.reduceWithResult(resultNonNull()) { result ->
                copy(
                    currentToken = result.data,
                    draftToken = result.data,
                    loading = false,
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
            flow {
                emit(Content.Loading())
                try {
                    require(isValidLocalMcpToken(draft)) {
                        "Token must be 32-256 URL-safe characters using letters, " +
                            "digits, dot, underscore, tilde or hyphen"
                    }
                    store.saveToken(draft)
                    emit(Content.Data(draft))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    emit(Content.Error(exception))
                }
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
        onTrigger {
            flow {
                emit(Content.Loading())
                try {
                    val token = store.regenerateToken()
                    emit(Content.Data(token))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    emit(Content.Error(exception))
                }
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
