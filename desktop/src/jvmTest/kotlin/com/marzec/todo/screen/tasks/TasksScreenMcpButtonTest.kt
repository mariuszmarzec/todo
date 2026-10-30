package com.marzec.todo.screen.tasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.marzec.todo.screen.tasks.model.TasksStore
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class TasksScreenMcpButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `MCP button renders when onOpenLocalMcpSettings is provided`() {
        val store = mockk<TasksStore>()
        var callbackInvoked = false
        val callback = { callbackInvoked = true }

        composeRule.setContent {
            TasksScreen(
                store = store,
                actionBarProvider = com.marzec.view.ActionBarProvider(mockk()),
                onOpenLocalMcpSettings = callback
            )
        }

        composeRule.onNodeWithText("MCP").assertIsDisplayed()
    }

    @Test
    fun `MCP button does not render when onOpenLocalMcpSettings is null`() {
        val store = mockk<TasksStore>()

        composeRule.setContent {
            TasksScreen(
                store = store,
                actionBarProvider = com.marzec.view.ActionBarProvider(mockk()),
                onOpenLocalMcpSettings = null
            )
        }

        composeRule.onNodeWithText("MCP").assertDoesNotExist()
    }
}
