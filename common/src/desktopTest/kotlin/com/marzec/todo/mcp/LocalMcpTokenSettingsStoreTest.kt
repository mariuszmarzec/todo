package com.marzec.todo.mcp

import com.marzec.common.CopyToClipBoardHelper
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalMcpTokenSettingsStoreTest {

    @Test
    fun `load token populates current and draft token`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val tokenStore = TestMcpTokenStore()
        val store = settingsStore(scope = this, tokenStore = tokenStore, copyHelper = noOpCopyHelper())

        store.loadToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        assertEquals(tokenStore.currentToken, data.currentToken)
        assertEquals(data.currentToken, data.draftToken)
        assertTrue(!data.loading)
        assertNull(data.error)
    }

    @Test
    fun `load token failure exposes error and stops loading`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val store = settingsStore(
                scope = this,
                tokenStore = FailingTokenStore(RuntimeException("boom")),
                copyHelper = noOpCopyHelper()
            )

        store.loadToken()
        advanceUntilIdle()

        val state = store.state.value
        assertTrue(state is State.Error)
        assertEquals("boom", (state as State.Error).message)
        assertTrue(state.data?.loading == true)
    }

    @Test
    fun `save token with valid draft updates current token and shows status`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val tokenStore = TestMcpTokenStore()
        val store = settingsStore(scope = this, tokenStore = tokenStore, copyHelper = noOpCopyHelper())
        val draft = "CustomToken-01234567890123456789012345678901"

        store.loadToken()
        store.onDraftTokenChanged(draft)
        store.saveToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        assertEquals(draft, data.currentToken)
        assertEquals(data.currentToken, data.draftToken)
        assertEquals("Token updated. The previous stored token has been replaced.", data.status)
        assertNull(data.error)
        assertTrue(!data.saving)
        assertEquals(draft, tokenStore.currentToken)
    }

    @Test
    fun `save token with invalid draft exposes error and keeps saving flag cleared`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val store = settingsStore(scope = this, tokenStore = TestMcpTokenStore(), copyHelper = noOpCopyHelper())

        store.loadToken()
        store.onDraftTokenChanged("short")
        store.saveToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        assertEquals("short", data.draftToken)
        assertEquals(
            "Token must be 32-256 URL-safe characters using letters, digits, dot, underscore, tilde or hyphen",
            data.error
        )
        assertTrue(!data.saving)
    }

    @Test
    fun `regenerate token updates current token and shows status`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val tokenStore = TestMcpTokenStore()
        val store = settingsStore(scope = this, tokenStore = tokenStore, copyHelper = noOpCopyHelper())
        val initial = tokenStore.currentToken

store.loadToken()
        store.regenerateToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        assertEquals(tokenStore.currentToken, data.currentToken)
        assertEquals(data.currentToken, data.draftToken)
        assertEquals("Token updated. The previous stored token has been replaced.", data.status)
        assertTrue(!data.saving)
        assertNotEquals(initial, data.currentToken)
    }

    @Test
    fun `draft token change clears error and status`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val store = settingsStore(
                scope = this,
                tokenStore = FailingTokenStore(RuntimeException("boom")),
                copyHelper = noOpCopyHelper()
            )

        store.loadToken()
        store.onDraftTokenChanged("new-draft")
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        assertEquals("new-draft", data.draftToken)
        assertNull(data.error)
        assertNull(data.status)
    }

    @Test
    fun `copy token triggers clipboard helper and shows status`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val copyHelper = mockk<CopyToClipBoardHelper>()
        every { copyHelper.copy(any()) } returns Unit
        val store = settingsStore(scope = this, tokenStore = TestMcpTokenStore(), copyHelper = copyHelper)

        store.loadToken()
        store.copyToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        verify { copyHelper.copy(data.currentToken) }
        assertEquals("Token copied to clipboard.", data.status)
    }

    private fun settingsStore(
        scope: kotlinx.coroutines.CoroutineScope,
        tokenStore: McpTokenStore,
        copyHelper: CopyToClipBoardHelper
    ) = LocalMcpTokenSettingsStore(
        scope = scope,
        store = tokenStore,
        copyToClipBoardHelper = copyHelper
    )

    private fun State<LocalMcpTokenSettingsState>.dataOrThrow(): LocalMcpTokenSettingsState =
        (this as? State.Data)?.data
            ?: (this as? State.Loading)?.data
            ?: (this as? State.Error)?.data
            ?: error("Expected data but was $this")
}

private class FailingTokenStore(private val exception: Exception) : McpTokenStore {
    override suspend fun getOrCreateToken(): String = throw exception
    override suspend fun saveToken(token: String) = Unit
    override suspend fun regenerateToken(): String = throw exception
}

private fun noOpCopyHelper(): CopyToClipBoardHelper = mockk<CopyToClipBoardHelper>().also {
    every { it.copy(any()) } returns Unit
}
