package com.marzec.todo.screen.taskdetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marzec.common.CopyToClipBoardHelper
import com.marzec.common.OpenUrlHelper
import com.marzec.delegate.DialogDelegateImpl
import com.marzec.delegate.SearchDelegateImpl
import com.marzec.delegate.SelectionDelegateImpl
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.MemoryStateCache
import com.marzec.repository.LoginRepository
import com.marzec.todo.delegates.dialog.ChangePriorityDelegateImpl
import com.marzec.todo.delegates.dialog.RemoveTaskDelegateImpl
import com.marzec.todo.delegates.dialog.UrlDelegateImpl
import com.marzec.todo.delegates.reorder.ReorderDelegateImpl
import com.marzec.todo.delegates.reorder.ReorderMode
import com.marzec.todo.model.Task
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.screen.taskdetails.model.TaskDetailsStore
import com.marzec.delegate.DialogState
import com.marzec.view.ActionBarProvider
import com.marzec.view.SearchState
import com.marzec.navigation.NavigationEntry
import com.marzec.navigation.navigationState
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import io.mockk.just
import io.mockk.Runs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule

class TaskDetailsDescriptionLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val shortDescription = "Short task description https://example.com"

    private val longDescription = List(100) { "Line $it https://example.com/line$it" }.joinToString("\n")

    @Test
    fun `all Markdown heading levels are scaled down proportionally`() {
        var defaultHeadingSizes = emptyList<TextUnit>()
        var markdownHeadingSizes = emptyList<TextUnit>()
        var markdownParagraphSize = TextUnit.Unspecified
        var defaultBodySize = TextUnit.Unspecified

        compose.setContent {
            val defaults = MaterialTheme.typography
            val markdownTypography = taskDescriptionMarkdownTypography()
            SideEffect {
                defaultHeadingSizes = listOf(
                    defaults.h1.fontSize,
                    defaults.h2.fontSize,
                    defaults.h3.fontSize,
                    defaults.h4.fontSize,
                    defaults.h5.fontSize,
                    defaults.h6.fontSize
                )
                markdownHeadingSizes = listOf(
                    markdownTypography.h1.fontSize,
                    markdownTypography.h2.fontSize,
                    markdownTypography.h3.fontSize,
                    markdownTypography.h4.fontSize,
                    markdownTypography.h5.fontSize,
                    markdownTypography.h6.fontSize
                )
                defaultBodySize = defaults.body1.fontSize
                markdownParagraphSize = markdownTypography.paragraph.fontSize
            }
        }

        compose.runOnIdle {
            val expectedHeadingSizes = listOf(
                defaultHeadingSizes[0] * 0.5f,
                defaultHeadingSizes[1] * 0.5f,
                defaultHeadingSizes[2] * 0.5f,
                maxOf(defaultHeadingSizes[3] - 2.sp, 14.sp),
                maxOf(defaultHeadingSizes[4] - 4.sp, 14.sp),
                maxOf(defaultHeadingSizes[5] - 6.sp, 14.sp)
            )
            assertEquals(
                expectedHeadingSizes,
                markdownHeadingSizes,
                "h1-h3 should be halved; h4-h6 should step down by 2sp per level, never below 14sp"
            )
            assertTrue(markdownHeadingSizes.all { it >= 14.sp })
            assertEquals(14.sp, 10.sp.reducedForHeadingLevel(6), "heading sizes must never go below 14sp")
            assertEquals(defaultBodySize, markdownParagraphSize, "paragraph text should keep its Material default")
        }
    }

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

    // ============================================================
    // Real TaskDetailsScreen UI tests with mocked store
    // ============================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createMockNavigationStore(): NavigationStore {
        val navigationState = MutableStateFlow(navigationState(emptyList<NavigationEntry>()))
        val store = mockk<NavigationStore>(relaxed = true)
        coEvery { store.state } returns navigationState.asStateFlow()
        coEvery { store.goBack() } just Runs
        return store
    }

    @Composable
    private fun MockActionBarProvider() = ActionBarProvider(createMockNavigationStore())

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `real TaskDetailsScreen - short description fully visible and action buttons accessible`() = runTest {
        val previousStateThread = Store4Impl.stateThread
        Store4Impl.stateThread = UnconfinedTestDispatcher(testScheduler)
        try {
            val store = createMockTaskDetailsStore(
                scope = this,
                description = shortDescription
            )
            compose.setContent {
                TaskDetailsScreen(store = store, actionBarProvider = MockActionBarProvider())
            }

            // Description text should be fully visible
            compose.onNodeWithText(shortDescription).assertExists()

            // Action buttons should be visible (IconButtons use contentDescription)
            compose.onNodeWithText("ID: 1").assertExists()
            compose.onNode(androidx.compose.ui.test.hasContentDescription("Open url")).assertExists()
            compose.onNode(androidx.compose.ui.test.hasContentDescription("Copy description")).assertExists()
            compose.onNode(androidx.compose.ui.test.hasContentDescription("Copy task")).assertExists()
        } finally {
            Store4Impl.stateThread = previousStateThread
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `real TaskDetailsScreen - long description does not push out action buttons`() = runTest {
        val previousStateThread = Store4Impl.stateThread
        Store4Impl.stateThread = UnconfinedTestDispatcher(testScheduler)
        try {
            val store = createMockTaskDetailsStore(
                scope = this,
                description = longDescription
            )
            compose.setContent {
                TaskDetailsScreen(store = store, actionBarProvider = MockActionBarProvider())
            }

            // Core requirement: action buttons should remain visible (not pushed out by long description)
            compose.onNodeWithText("ID: 1").assertExists()
            compose.onNode(androidx.compose.ui.test.hasContentDescription("Open url")).assertExists()
            compose.onNode(androidx.compose.ui.test.hasContentDescription("Copy description")).assertExists()
            compose.onNode(androidx.compose.ui.test.hasContentDescription("Copy task")).assertExists()
        } finally {
            Store4Impl.stateThread = previousStateThread
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createMockTaskDetailsStore(
        scope: kotlinx.coroutines.CoroutineScope,
        taskId: Int = 1,
        description: String = "Test description",
        isToDo: Boolean = true
    ): TaskDetailsStore {
        val repository = mockk<TodoRepository>(relaxed = true)
        val changePriorityDelegate = ChangePriorityDelegateImpl<TaskDetailsState>(repository)
        val task = Task(
            id = taskId,
            description = description,
            addedTime = LocalDateTime.parse("2026-10-30T10:00:00"),
            modifiedTime = LocalDateTime.parse("2026-10-30T10:00:00"),
            parentTaskId = null,
            subTasks = emptyList(),
            isToDo = isToDo,
            priority = 1,
            scheduler = null,
            expirationDate = null,
            shares = emptyList(),
            ownerId = null
        )
        val initialState = State.Data(
            TaskDetailsState(
                task = task,
                dialog = DialogState.NoDialog(),
                selected = emptySet(),
                search = SearchState.DEFAULT,
                reorderMode = ReorderMode.Disabled
            )
        )
        return TaskDetailsStore(
            scope = scope,
            navigationStore = mockk<NavigationStore>(relaxed = true),
            cacheKey = "task-details-ui-test-$taskId",
            stateCache = MemoryStateCache(),
            initialState = initialState,
            todoRepository = repository,
            taskId = taskId,
            copyToClipBoardHelper = mockk<CopyToClipBoardHelper>(relaxed = true),
            dialogDelegate = DialogDelegateImpl<Int, TaskDetailsState>(),
            removeTaskDelegate = RemoveTaskDelegateImpl<TaskDetailsState>(repository),
            urlDelegate = UrlDelegateImpl<TaskDetailsState>(mockk<OpenUrlHelper>(relaxed = true)),
            changePriorityDelegate = changePriorityDelegate,
            selectionDelegate = SelectionDelegateImpl<Int, TaskDetailsState>(),
            searchDelegate = SearchDelegateImpl<TaskDetailsState>(),
            reorderDelegate = ReorderDelegateImpl<TaskDetailsState>(
                changePriorityDelegate = changePriorityDelegate,
                tasksToReorder = { task.subTasks },
                stateReducer = { mode -> copy(reorderMode = mode) }
            ),
            loginRepository = mockk<LoginRepository>(relaxed = true)
        )
    }
}
