package com.marzec.todo.screen.addnewtask.model

import com.marzec.content.Content
import com.marzec.content.ifDataSuspend
import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.mvi.mapData
import com.marzec.mvi.reduceContentNoChanges
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceDataWithContent
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationOptions
import com.marzec.navigation.NavigationStore
import com.marzec.preferences.Preferences
import com.marzec.time.currentTime
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.TodoDestination
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.LocalDateTime

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
                    todoRepository.observeTask(it)
                }
            }
        }
        reducer {
            result?.let {
                state.reduceDataWithContent(
                    result = resultNonNull(),
                    defaultData = AddNewTaskState.initial(0, null)
                ) { result ->
                    copy(
                        taskId = result.data.id,
                        parentTaskId = result.data.parentTaskId,
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
                    TodoDestination.TaskDetails(taskIdToShow)
                } else {
                    TodoDestination.Tasks
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

    fun onScheduleButtonClick() = sideEffect {
        state.ifDataAvailable {
            navigationStore.next(
                NavigationAction(
                    TodoDestination.Schedule(scheduler)
                )
            )
        }
    }

    fun onRemoveSchedulerButtonClick() = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(scheduler = null)
            }
        }
    }
}
