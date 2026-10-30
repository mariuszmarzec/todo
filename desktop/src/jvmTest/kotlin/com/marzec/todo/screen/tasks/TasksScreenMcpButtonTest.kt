package com.marzec.todo.screen.tasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.marzec.todo.screen.tasks.model.TasksStore
import com.marzec.view.ActionBarProvider
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class TasksScreenMcpButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `MCP button renders and clicking it invokes the navigation callback`() {
        val store = mockk<TasksStore>()
        var callbackInvoked = false
        val callback = { callbackInvoked = true }

        composeRule.setContent {
            TasksScreen(
                store = store,
                actionBarProvider = ActionBarProvider(mockk()),
                onOpenLocalMcpSettings = callback
            )
        }

        composeRule.onNodeWithText("MCP").assertIsDisplayed()
        composeRule.onNodeWithText("MCP").performClick()

        assert(callbackInvoked)
    }

    @Test
    fun `MCP button does not render when onOpenLocalMcpSettings is null`() {
        val store = mockk<TasksStore>()

        composeRule.setContent {
            TasksScreen(
                store = store,
                actionBarProvider = ActionBarProvider(mockk()),
                onOpenLocalMcpSettings = null
            )
        }

        composeRule.onNodeWithText("MCP").assertDoesNotExist()
    }
}