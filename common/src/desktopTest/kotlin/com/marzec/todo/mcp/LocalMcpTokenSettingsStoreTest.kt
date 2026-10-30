package com.marzec.todo.mcp

import com.marzec.common.CopyToClipBoardHelper
import com.marzec.mvi.Store4Impl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestDispatcher
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
    fun `copy token triggers clipboard helper and shows status`() = runTest {
        val dispatcher: TestDispatcher = UnconfinedTestDispatcher(testScheduler)
        Store4Impl.stateThread = dispatcher
        val copyHelper = mockk<CopyToClipBoardHelper>()
        every { copyHelper.copy(any()) } returns Unit
        val store = settingsStore(scope = this, tokenStore = TestMcpTokenRepository(), copyHelper = copyHelper)

        store.loadToken()
        store.copyToken()
        advanceUntilIdle()

        val data = store.state.value.dataOrThrow()
        verify { copyHelper.copy(data.currentToken) }
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

    private fun kotlinx.coroutines.test.TestScope.settingsStore(
        tokenStore: McpTokenRepository,
        copyHelper: CopyToClipBoardHelper
    ) = LocalMcpTokenSettingsStore(
        scope = this,
        store = tokenStore,
        copyToClipBoardHelper = copyHelper
    )

    private fun com.marzec.mvi.State<LocalMcpTokenSettingsState>.dataOrThrow(): LocalMcpTokenSettingsState =
        (this as? com.marzec.mvi.State.Data)?.data
            ?: (this as? com.marzec.mvi.State.Loading)?.data
            ?: (this as? com.marzec.mvi.State.Error)?.data
            ?: error("Expected data but was $this")
}

private class FailingTokenStore(private val exception: Exception) : McpTokenRepository {
    override suspend fun getOrCreateToken(): String = throw exception
    override suspend fun saveToken(token: String) = Unit
    override suspend fun regenerateToken(): String = throw exception
}

private fun noOpCopyHelper(): CopyToClipBoardHelper = mockk<CopyToClipBoardHelper>().also {
    every { it.copy(any()) } returns Unit
}
