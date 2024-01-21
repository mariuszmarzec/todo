package com.marzec.todo.screen.addsubtask.model

import com.marzec.content.Content
import com.marzec.content.ifDataSuspend
import com.marzec.content.mapData
import com.marzec.delegate.SearchDelegate
import com.marzec.delegate.delegates
import com.marzec.extensions.asInstance
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.mvi.reduceContentToLoadingWithNoChanges
import com.marzec.mvi.reduceDataWithContent
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.next
import com.marzec.preferences.Preferences
import com.marzec.delegate.SelectionDelegate
import com.marzec.todo.extensions.findRootIdOrNull
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.TodoDestination
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class AddSubTaskStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<AddSubTaskData>,
    private val todoRepository: TodoRepository,
    private val taskId: Int,
    selectionDelegate: SelectionDelegate<Int>,
    searchDelegate: SearchDelegate
) : Store3<State<AddSubTaskData>>(
    scope, stateCache.get(cacheKey) ?: initialState
), SelectionDelegate<Int> by selectionDelegate, SearchDelegate by searchDelegate {

    init {
        delegates(selectionDelegate, searchDelegate)
    }

    fun initialLoad() = intent<Content<List<Task>>> {
        onTrigger {
            todoRepository.observeTasks().map { content ->
                content.mapData { tasks ->
                    val rootId = tasks.findRootIdOrNull(taskId)
                    tasks.filterNot { it.id == rootId }
                }
            }
        }
        reducer {
            state.reduceDataWithContent(resultNonNull()) { tasks ->
                this?.copy(tasks = tasks) ?: AddSubTaskData.DEFAULT
            }
        }
    }

    fun onAddSubTaskClick() = sideEffect {
        navigationStore.next(
            TodoDestination.AddNewTask(
                taskToEditId = null,
                parentTaskId = taskId
            )
        )
    }

    fun pinSubtask(id: Int) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                val newParentTaskId = taskId
                tasks.firstOrNull { id == it.id }?.let { task ->
                    todoRepository.pinTask(
                        task = task,
                        parentTaskId = newParentTaskId,
                    )
                }
            }
        }

        reducer {
            state.reduceContentToLoadingWithNoChanges(resultNonNull())
        }

        sideEffect {
            result?.asInstance<Content.Data<*>> {
                navigationStore.goBack()
            }
        }
    }

    override suspend fun onNewState(newState: State<AddSubTaskData>) {
        stateCache.set(cacheKey, newState)
    }

    fun onPinAllSelectedClicked() = intent<Content<Unit>>("pinAllSelected") {
        onTrigger {
            state.ifDataAvailable {
                todoRepository.pinAllTasks(
                    tasks = tasks.filter { it.id in selected },
                    parentTaskId = taskId
                )
            }
        }

        cancelTrigger(runSideEffectAfterCancel = true) {
            resultNonNull() is Content.Data
        }

        reducer {
            state.reduceContentToLoadingWithNoChanges(resultNonNull())
        }

        sideEffect {
            resultNonNull().ifDataSuspend {
                navigationStore.goBack()
            }
        }
    }
}
