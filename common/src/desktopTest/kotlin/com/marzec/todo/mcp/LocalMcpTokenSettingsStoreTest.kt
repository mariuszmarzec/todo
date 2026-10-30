package com.marzec.todo.mcp

import com.marzec.common.CopyToClipBoardHelper
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.State
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LocalMcpTokenSettingsStoreTest {

    @Test
    fun `load token populates current and draft token`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val tokenStore = TestMcpTokenRepository()
        val store = settingsStore(scope = this, tokenStore = tokenStore, copyHelper = noOpCopyHelper())

        store.loadToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        assertEquals(tokenStore.currentToken, data.currentToken)
        assertEquals(data.currentToken, data.draftToken)
        assertNull(data.error)
    }

    @Test
    fun `load token failure surfaces error state`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val tokenStore = FailingTokenStore(RuntimeException("boom"))
        val store = settingsStore(scope = this, tokenStore = tokenStore, copyHelper = noOpCopyHelper())

        store.loadToken()
        advanceUntilIdle()

        val errorState = store.state.value as State.Error<LocalMcpTokenSettingsState>
        assertEquals("boom", errorState.message)
        assertEquals("", errorState.data?.currentToken)
        assertEquals("", errorState.data?.draftToken)
    }

    @Test
    fun `save token with valid draft updates current token and shows status`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val tokenStore = TestMcpTokenRepository()
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
    fun `save token with invalid draft leaves state unchanged and records error`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val tokenStore = TestMcpTokenRepository()
        val store = settingsStore(scope = this, tokenStore = tokenStore, copyHelper = noOpCopyHelper())
        val invalidDraft = "short"

        store.loadToken()
        advanceUntilIdle()
        val loadedToken = tokenStore.currentToken

        store.onDraftTokenChanged(invalidDraft)
        store.saveToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        assertEquals(invalidDraft, data.draftToken)
        assertEquals(loadedToken, data.currentToken)
        assertTrue(data.error != null)
        assertTrue(!data.saving)
        assertEquals(loadedToken, tokenStore.currentToken)
    }

    @Test
    fun `save token failure clears saving and records the error`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val tokenStore = SaveFailingTokenRepository()
        val store = settingsStore(scope = this, tokenStore = tokenStore, copyHelper = noOpCopyHelper())
        val draft = "CustomToken-01234567890123456789012345678901"

        store.loadToken()
        store.onDraftTokenChanged(draft)
        store.saveToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        assertEquals("save failed", data.error)
        assertTrue(!data.saving)
        assertEquals(tokenStore.originalToken, data.currentToken)
        assertEquals(tokenStore.originalToken, tokenStore.currentToken)
    }

    @Test
    fun `token content flow propagates cancellation`() = runTest {
        val exception = CancellationException("cancelled")

        val thrown = assertFailsWith<CancellationException> {
            FailingTokenStore(exception).getOrCreateTokenContent().toList()
        }

        assertEquals("cancelled", thrown.message)
    }

    @Test
    fun `regenerate token updates current token and shows status`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val tokenStore = TestMcpTokenRepository()
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
            tokenStore = TestMcpTokenRepository(),
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
    fun `copy token shows status`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val copyHelper = noOpCopyHelper()
        val store = settingsStore(scope = this, tokenStore = TestMcpTokenRepository(), copyHelper = copyHelper)

        store.loadToken()
        advanceUntilIdle()
        store.copyToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        assertEquals("Token copied to clipboard.", data.status)
    }

    private fun settingsStore(
        scope: kotlinx.coroutines.CoroutineScope,
        tokenStore: McpTokenRepository,
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

private class FailingTokenStore(private val exception: Exception) : McpTokenRepository {
    override suspend fun getOrCreateToken(): String = throw exception
    override suspend fun saveToken(token: String) = Unit
    override suspend fun regenerateToken(): String = throw exception
}

private class SaveFailingTokenRepository : McpTokenRepository {
    private val delegate = TestMcpTokenRepository()
    val originalToken = delegate.currentToken
    val currentToken: String
        get() = delegate.currentToken

    override suspend fun getOrCreateToken(): String = delegate.getOrCreateToken()
    override suspend fun saveToken(token: String) {
        throw RuntimeException("save failed")
    }
    override suspend fun regenerateToken(): String = delegate.regenerateToken()
}

private fun noOpCopyHelper(): CopyToClipBoardHelper = mockk<CopyToClipBoardHelper>().also {
    every { it.copy(any()) } returns Unit
}