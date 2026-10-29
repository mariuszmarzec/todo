package com.marzec.todo.mcp

import com.marzec.content.Content
import com.marzec.featuretoggle.FeatureTogglesManager
import com.marzec.model.User
import com.marzec.repository.LoginRepository
import com.marzec.todo.repository.TodoRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalMcpServerLifecycleControllerTest {

    @Test
    fun startsOnlyWhenEnabledAndLoggedInAndStopsForLifecycleChanges() = runBlocking {
        val enabled = MutableStateFlow(false)
        val session = MutableStateFlow<com.marzec.content.Content<User>>(
            Content.Error(IllegalStateException("logged out"))
        )
        val featureTogglesManager = mockk<FeatureTogglesManager>()
        every { featureTogglesManager.observe(LocalMcpServerLifecycleController.FEATURE_TOGGLE) } returns enabled
        val loginRepository = mockk<LoginRepository>()
        coEvery { loginRepository.observeCurrentUser() } returns session
        val todoRepository = mockk<TodoRepository>()
        val tokenStore = LifecycleTestTokenStore()
        val instances = mutableListOf<FakeLocalMcpServer>()
        val controller = LocalMcpServerLifecycleController(
            featureTogglesManager = featureTogglesManager,
            loginRepository = loginRepository,
            todoRepository = todoRepository,
            tokenStore = tokenStore,
            port = 8765,
            dispatcher = Dispatchers.Unconfined,
            serverFactory = { _, _, _ -> FakeLocalMcpServer().also(instances::add) }
        )

        try {
            controller.start()
            controller.start()
            enabled.value = true
            session.value = Content.Data(User(1, "mock@user.com"))
            awaitCondition { instances.size == 1 && instances[0].startCount == 1 }
            assertTrue(instances[0].stopCount == 0)

            session.value = Content.Error(IllegalStateException("logged out"))
            awaitCondition { instances[0].stopCount == 1 }

            session.value = Content.Data(User(1, "mock@user.com"))
            awaitCondition { instances.size == 2 && instances[1].startCount == 1 }

            enabled.value = false
            awaitCondition { instances[1].stopCount == 1 }

            enabled.value = true
            awaitCondition { instances.size == 3 && instances[2].startCount == 1 }
            controller.close()
            controller.close()
            assertEquals(1, instances[2].stopCount)
        } finally {
            controller.close()
        }
    }

    private suspend fun awaitCondition(condition: () -> Boolean) {
        withTimeout(2_000) {
            while (!condition()) yield()
        }
    }

    private class FakeLocalMcpServer : LocalMcpServerInstance {
        var startCount = 0
        var stopCount = 0

        override fun start() {
            startCount++
        }

        override fun stop() {
            stopCount++
        }
    }

    private class LifecycleTestTokenStore : McpTokenStore {
        override suspend fun getOrCreateToken(): String = "lifecycle-test-token-012345678901234567"

        override suspend fun saveToken(token: String) = Unit

        override suspend fun regenerateToken(): String = getOrCreateToken()
    }
}
