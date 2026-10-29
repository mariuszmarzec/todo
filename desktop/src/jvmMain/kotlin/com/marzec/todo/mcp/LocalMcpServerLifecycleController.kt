package com.marzec.todo.mcp

import com.marzec.content.Content
import com.marzec.featuretoggle.FeatureTogglesManager
import com.marzec.repository.LoginRepository
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean

class LocalMcpServerLifecycleController(
    private val featureTogglesManager: FeatureTogglesManager,
    private val loginRepository: LoginRepository,
    private val todoRepository: TodoRepository,
    private val tokenStore: McpTokenStore,
    private val port: Int = DEFAULT_PORT,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val serverFactory: (Int, TodoRepository, McpTokenStore) -> LocalMcpServerInstance = { serverPort, repository, store ->
        TodoMcpServer(serverPort, store, repository)
    }
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val serverMutex = Mutex()
    private val started = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private var activeServer: LocalMcpServerInstance? = null

    fun start() {
        if (closed.get() || !started.compareAndSet(false, true)) return
        scope.launch {
            observeLifecycle()
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        scope.cancel()
        runBlocking {
            serverMutex.withLock {
                stopActiveServer()
            }
        }
    }

    private suspend fun observeLifecycle() {
        val enabledFlow: Flow<Boolean> = try {
            featureTogglesManager.observe(FEATURE_TOGGLE)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            flowOf(false)
        }
        val userFlow: Flow<Boolean> = try {
            loginRepository.observeCurrentUser().map { it is Content.Data }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            flowOf(false)
        }

        combine(
            enabledFlow.catch { emit(false) },
            userFlow.catch { emit(false) }
        ) { enabled, hasUser ->
            enabled && hasUser
        }.distinctUntilChanged().collect { shouldRun ->
            try {
                reconcile(shouldRun)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun reconcile(shouldRun: Boolean) {
        serverMutex.withLock {
            if (closed.get()) {
                stopActiveServer()
                return@withLock
            }
            if (!shouldRun) {
                stopActiveServer()
                return@withLock
            }
            if (activeServer != null) return@withLock
            tokenStore.getOrCreateToken()
            val server = serverFactory(port, todoRepository, tokenStore)
            try {
                server.start()
                activeServer = server
            } catch (error: Exception) {
                runCatching { server.stop() }
                throw error
            }
        }
    }

    private fun stopActiveServer() {
        val server = activeServer ?: return
        activeServer = null
        server.stop()
    }

    companion object {
        const val FEATURE_TOGGLE = "todo.localMcpServer"
        const val DEFAULT_PORT = 8765
    }
}
