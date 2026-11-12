package com.marzec.todo.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule

class ResizableScrollableContentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun contentLambdaIsRenderedAndShortContentDoesNotShowResizeHandle() {
        compose.setContent {
            Box(
                Modifier
                    .size(width = 320.dp, height = 400.dp)
                    .testTag("testRoot")
            ) {
                ResizableScrollableContent(
                    modifier = Modifier.fillMaxSize(),
                    hasSubtasks = true
                ) {
                    Column {
                        Text("Composable lambda content")
                        Text("Second content element")
                    }
                }
            }
        }

        compose.onNodeWithText("Composable lambda content").assertExists()
        compose.onNodeWithText("Second content element").assertExists()
        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).assertDoesNotExist()
    }

    @Test
    fun overflowingContentIsCappedAndCanBeResizedInBothDirections() {
        compose.setContent {
            Box(Modifier.size(width = 320.dp, height = 400.dp)) {
                ResizableScrollableContent(
                    modifier = Modifier.fillMaxSize(),
                    hasSubtasks = true
                ) {
                    Column(Modifier.height(240.dp)) {
                        repeat(12) { index ->
                            Text("Scrollable line $index")
                        }
                    }
                }
            }
        }

        compose.waitForIdle()

        val initialHeight = viewportHeight()
        val parentHeight = compose.onNodeWithTag("testRoot")
            .fetchSemanticsNode().boundsInRoot.height
        assertTrue(initialHeight in (parentHeight / 2f - 1f)..(parentHeight / 2f + 1f))
        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).assertExists()
        compose.onNodeWithTag(RESIZABLE_CONTENT_VIEWPORT_TAG).assert(hasScrollAction())

        // Expanding beyond the natural content height must not hide the handle:
        // content is still larger than the original 50% cap.
        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, 150f))
            up()
        }
        compose.waitForIdle()

        val expandedHeight = viewportHeight()
        val handleHeight = compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG)
            .fetchSemanticsNode().boundsInRoot.height
        assertTrue(expandedHeight > initialHeight)
        assertTrue(expandedHeight + handleHeight <= parentHeight * 0.75f + 1f)
        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).assertExists()

        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, -60f))
            up()
        }
        compose.waitForIdle()

        val reducedHeight = viewportHeight()
        assertTrue(reducedHeight < expandedHeight)
        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).assertExists()
    }

    @Test
    fun taskWithoutSubtasksUsesAvailableHeightAndNeverShowsResizeHandle() {
        compose.setContent {
            Box(
                Modifier
                    .size(width = 320.dp, height = 400.dp)
                    .testTag("testRoot")
            ) {
                ResizableScrollableContent(
                    modifier = Modifier.fillMaxSize(),
                    hasSubtasks = false
                ) {
                    Box(Modifier.fillMaxWidth().height(600.dp)) {
                        Text("Long content without subtasks")
                    }
                }
            }
        }

        compose.waitForIdle()

        val parentHeight = compose.onNodeWithTag("testRoot")
            .fetchSemanticsNode().boundsInRoot.height
        val contentViewportHeight = viewportHeight()

        assertEquals(parentHeight, contentViewportHeight)
        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).assertDoesNotExist()
        compose.onNodeWithTag(RESIZABLE_CONTENT_VIEWPORT_TAG).assert(hasScrollAction())
    }

    @Test
    fun changingSubtaskModeResetsManuallyResizedHeight() {
        val hasSubtasks = mutableStateOf(true)

        compose.setContent {
            Box(
                Modifier
                    .size(width = 320.dp, height = 400.dp)
                    .testTag("testRoot")
            ) {
                ResizableScrollableContent(
                    modifier = Modifier.fillMaxSize(),
                    hasSubtasks = hasSubtasks.value
                ) {
                    Box(Modifier.fillMaxWidth().height(240.dp))
                }
            }
        }

        compose.waitForIdle()
        val initialHeight = viewportHeight()

        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, 150f))
            up()
        }
        compose.waitForIdle()
        val expandedHeight = viewportHeight()
        assertTrue(expandedHeight > initialHeight)

        compose.runOnIdle {
            hasSubtasks.value = false
        }
        compose.waitForIdle()

        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).assertDoesNotExist()
        val fullHeight = viewportHeight()
        assertEquals(
            compose.onNodeWithTag("testRoot").fetchSemanticsNode().boundsInRoot.height,
            fullHeight
        )

        compose.runOnIdle {
            hasSubtasks.value = true
        }
        compose.waitForIdle()

        val resetHeight = viewportHeight()
        assertTrue(resetHeight < expandedHeight)
        compose.onNodeWithTag(RESIZABLE_CONTENT_HANDLE_TAG).assertExists()
    }

    private fun viewportHeight(): Float = compose
        .onNodeWithTag(RESIZABLE_CONTENT_VIEWPORT_TAG)
        .fetchSemanticsNode()
        .boundsInRoot
        .height
}
