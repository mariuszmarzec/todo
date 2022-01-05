package com.marzec.todo.screen.addnewtask.model

import com.marzec.mvi.State
import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.Store3
import com.marzec.mvi.reduceContentNoChanges
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationAction
import com.marzec.todo.navigation.model.NavigationOptions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.network.Content
import com.marzec.todo.network.ifDataSuspend
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope

class AddNewTaskStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<AddNewTaskState>,
    private val todoRepository: TodoRepository,
) : Store3<State<AddNewTaskState>>(
    scope, stateCache.get(cacheKey) ?: initialState
) {

    fun initialLoad() = intent<Content<Task>> {
        onTrigger {
            state.ifDataAvailable {
                taskId?.let {
                    todoRepository.observeTask(listId, it)
                }
            }
        }
        reducer {
            result?.let {
                state.reduceDataWithContent(
                    result = resultNonNull(),
                    defaultData = AddNewTaskState.initial(0, null, null)
                ) { result ->
                    copy(
                        taskId = result.data.id,
                        parentTaskId = result.data.parentTaskId,
                        listId = result.data.listId,
                        description = result.data.description,
                        priority = result.data.priority,
                        isToDo = result.data.isToDo
                    )
                }
            } ?: state
        }
    }

    fun onDescriptionChanged(description: String) = intent<Unit> {
        reducer {
            state.reduceData { copy(description = description) }
        }
    }

    fun addNewTask() = intent<Content<Unit>>("addNewTask") {
        onTrigger {
            state.ifDataAvailable {
                val taskId = taskId
                if (taskId != null) {
                    todoRepository.updateTask(
                        taskId = taskId,
                        description = description,
                        parentTaskId = parentTaskId,
                        priority = priority,
                        isToDo = isToDo
                    )
                } else {
                    todoRepository.addNewTask(
                        listId,
                        description,
                        parentTaskId,
                        highestPriorityAsDefault
                    )
                }
            }
        }

        cancelTrigger(runSideEffectAfterCancel = true) {
            resultNonNull() is Content.Data
        }

        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }

        sideEffect {
            navigateOutAfterCall()
        }
    }

    fun addManyTasks() = intent<Content<Unit>>("addManyTasks") {
        onTrigger {
            state.ifDataAvailable {
                todoRepository.addNewTasks(
                    listId = listId,
                    highestPriorityAsDefault = highestPriorityAsDefault,
                    parentTaskId = parentTaskId,
                    descriptions = description.split("\n")
                )
            }
        }

        cancelTrigger(runSideEffectAfterCancel = true) {
            resultNonNull() is Content.Data
        }

        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }

        sideEffect {
            navigateOutAfterCall()
        }
    }

    fun toggleHighestPriority() = intent<Unit> {
        reducer {
            state.reduceData { copy(highestPriorityAsDefault = !highestPriorityAsDefault) }
        }
    }

    private suspend fun IntentBuilder.IntentContext<State<AddNewTaskState>, Content<Unit>>.navigateOutAfterCall() {
        result?.ifDataSuspend {
            state.ifDataAvailable(blockOnLoading = false) {
                val taskIdToShow = parentTaskId ?: taskId
                val destination = if (taskIdToShow != null) {
                    Destination.TaskDetails(listId, taskIdToShow)
                } else {
                    Destination.Tasks(listId)
                }
                navigationStore.next(
                    NavigationAction(
                        destination = destination,
                        options = NavigationOptions(destination, true)
                    )
                )
            }
        }
    }

    override suspend fun onNewState(newState: State<AddNewTaskState>) {
        stateCache.set(cacheKey, newState)
    }
}
