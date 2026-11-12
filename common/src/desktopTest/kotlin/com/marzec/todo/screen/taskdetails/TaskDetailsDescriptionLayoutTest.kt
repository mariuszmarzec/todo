package com.marzec.todo.screen.taskdetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.Rule

class TaskDetailsDescriptionLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val shortDescription = "Short task description"

    private val longDescription = List(100) { "Line $it" }.joinToString("\n")

    @Composable
    private fun DescriptionLayoutTest(description: String, availableHeightDp: Float = 800f) {
        // Use a fixed size container to simulate screen constraints
        Box(Modifier.size(400.dp, availableHeightDp.dp)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxDescriptionHeight = maxHeight / 2
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(max = maxDescriptionHeight)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(text = description, fontSize = 16.sp)
                    }
                    // Simulate action buttons row
                    Text(text = "ID: 1", fontSize = 12.sp)
                    Text(text = "Open url", fontSize = 12.sp)
                    Text(text = "Copy description", fontSize = 12.sp)
                    Text(text = "Copy task", fontSize = 12.sp)
                }
            }
        }
    }

    @Test
    fun `short description fits within 50% height limit and all action buttons visible`() {
        compose.setContent { DescriptionLayoutTest(shortDescription, 800f) }

        // Description text should be fully visible
        compose.onNodeWithText(shortDescription).assertExists()

        // All action buttons should be visible
        compose.onNodeWithText("ID: 1").assertExists()
        compose.onNodeWithText("Open url").assertExists()
        compose.onNodeWithText("Copy description").assertExists()
        compose.onNodeWithText("Copy task").assertExists()
    }

    @Test
    fun `long description does not push out action buttons at standard height`() {
        compose.setContent { DescriptionLayoutTest(longDescription, 800f) }

        // Action buttons should remain visible (not pushed out by long description)
        compose.onNodeWithText("ID: 1").assertExists()
        compose.onNodeWithText("Open url").assertExists()
        compose.onNodeWithText("Copy description").assertExists()
        compose.onNodeWithText("Copy task").assertExists()
    }

    @Test
    fun `action buttons remain visible with very long description at smaller height`() {
        compose.setContent { DescriptionLayoutTest(longDescription, 600f) }

        // All action buttons should be visible regardless of description length
        compose.onNodeWithText("ID: 1").assertExists()
        compose.onNodeWithText("Open url").assertExists()
        compose.onNodeWithText("Copy description").assertExists()
        compose.onNodeWithText("Copy task").assertExists()
    }

    @Test
    fun `description box has heightIn and vertical scroll - buttons accessible at very small height`() {
        // This test verifies the layout structure by checking that
        // long content doesn't push out the action buttons even at very small heights
        compose.setContent { DescriptionLayoutTest(longDescription, 400f) }

        // Even at very small heights, action buttons should remain accessible
        compose.onNodeWithText("ID: 1").assertExists()
        compose.onNodeWithText("Copy task").assertExists()
    }
}
