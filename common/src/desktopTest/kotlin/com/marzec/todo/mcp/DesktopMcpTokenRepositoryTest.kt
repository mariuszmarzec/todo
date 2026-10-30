package com.marzec.todo.mcp

import java.util.UUID
import java.util.prefs.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DesktopMcpTokenRepositoryTest {

    @Test
    fun tokenIsCreatedPersistedAndCanBeChangedOrRegenerated() = runBlocking {
        val preferences = Preferences.userRoot().node("com/marzec/todo/local-mcp-test-${UUID.randomUUID()}")

        try {
            val store = DesktopMcpTokenRepository(preferences)
            val tokens = coroutineScope {
                (1..8).map {
                    async(Dispatchers.Default) { store.getOrCreateToken() }
                }.awaitAll()
            }
            val initial = tokens.first()

            assertTrue(tokens.all { it == initial })
            assertTrue(isValidLocalMcpToken(initial))
            assertEquals(initial, store.getOrCreateToken())

            val regenerated = store.regenerateToken()
            assertNotEquals(initial, regenerated)
            assertEquals(regenerated, store.getOrCreateToken())

            val custom = "CustomToken-${UUID.randomUUID()}"
            store.saveToken(custom)
            assertEquals(custom, store.getOrCreateToken())

            val invalidResult = runCatching { store.saveToken("short") }
            assertTrue(invalidResult.isFailure)
        } finally {
            preferences.removeNode()
        }
    }
}
