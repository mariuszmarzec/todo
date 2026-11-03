package com.marzec.todo.screen.taskdetails

import com.marzec.common.CopyToClipBoardHelper
import com.marzec.common.OpenUrlHelper
import com.marzec.delegate.DialogDelegateImpl
import com.marzec.delegate.SearchDelegateImpl
import com.marzec.delegate.SelectionDelegateImpl
import com.marzec.mvi.State
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
import com.marzec.view.SearchState
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test

class TaskDetailsStoreTest {

    @Test
    fun copyTaskIdCopiesOnlyTheNumericId() = runTest {
        val taskId = 42017
        val clipboard = mockk<CopyToClipBoardHelper>(relaxed = true)
        val repository = mockk<TodoRepository>(relaxed = true)
        val changePriorityDelegate = ChangePriorityDelegateImpl<TaskDetailsState>(repository)
        val task = Task(
            id = taskId,
            description = "A task",
            addedTime = LocalDateTime.parse("2026-10-30T10:00:00"),
            modifiedTime = LocalDateTime.parse("2026-10-30T10:00:00"),
            parentTaskId = null,
            subTasks = emptyList(),
            isToDo = true,
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
        val store = TaskDetailsStore(
            scope = this,
            navigationStore = mockk<NavigationStore>(relaxed = true),
            cacheKey = "task-details-copy-id-test",
            stateCache = MemoryStateCache(),
            initialState = initialState,
            todoRepository = repository,
            taskId = taskId,
            copyToClipBoardHelper = clipboard,
            dialogDelegate = DialogDelegateImpl<Int, TaskDetailsState>(),
            removeTaskDelegate = RemoveTaskDelegateImpl(repository),
            urlDelegate = UrlDelegateImpl<TaskDetailsState>(mockk<OpenUrlHelper>(relaxed = true)),
            changePriorityDelegate = changePriorityDelegate,
            selectionDelegate = SelectionDelegateImpl<Int, TaskDetailsState>(),
            searchDelegate = SearchDelegateImpl<TaskDetailsState>(),
            reorderDelegate = ReorderDelegateImpl(
                changePriorityDelegate = changePriorityDelegate,
                tasksToReorder = { task.subTasks },
                stateReducer = { mode -> copy(reorderMode = mode) }
            ),
            loginRepository = mockk<LoginRepository>(relaxed = true)
        )

        store.copyTaskId()
        advanceUntilIdle()

        verify(exactly = 1) { clipboard.copy("42017") }
    }
}
