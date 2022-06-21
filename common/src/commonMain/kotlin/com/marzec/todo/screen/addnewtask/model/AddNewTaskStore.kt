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
import com.marzec.navigation.ResultCache
import com.marzec.preferences.Preferences
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.ifFalse
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.TodoDestination
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.screen.scheduler.RESULT_KEY_SCHEDULER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull

class AddNewTaskStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    private val resultCache: ResultCache,
    private val initialState: State<AddNewTaskState>,
    private val todoRepository: TodoRepository,
) : Store3<State<AddNewTaskState>>(
    scope, stateCache.get(cacheKey) ?: initialState
) {

    fun initialLoad() = (stateCache.get(cacheKey) ?: initialState)
        .asInstance<State.Loading<AddNewTaskState>> {
            intent<Content<Task>>("load") {
                onTrigger {
                    state.ifDataAvailable(blockOnLoading = false) {
                        taskId?.let {
                            todoRepository.observeTask(it)
                        }
                    }
                }
                reducer {
                    result?.let {
                        state.reduceDataWithContent(
                            result = resultNonNull(),
                            defaultData = AddNewTaskState.default(0, null)
                        ) { result ->
                            copy(
                                taskId = result.data.id,
                                parentTaskId = result.data.parentTaskId,
                                description = result.data.description,
                                priority = result.data.priority,
                                isToDo = result.data.isToDo,
                                scheduler = result.data.scheduler,
                                highestPriorityAsDefault = result.data.scheduler?.highestPriorityAsDefault ?: Scheduler.HIGHEST_PRIORITY_AS_DEFAULT,
                                removeAfterSchedule = (result.data.scheduler as? Scheduler.OneShot)?.removeScheduled ?: Scheduler.REMOVE_SCHEDULED
                            )
                        }
                    } ?: state
                }
            }
        }

    fun onSchedulerRequest() = intent("onSchedulerRequest") {
        onTrigger {
            resultCache.observe<Scheduler>(cacheKey, RESULT_KEY_SCHEDULER).filterNotNull()
        }

        reducer {
            state.reduceData { copy(scheduler = resultNonNull()) }
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
                        isToDo = isToDo,
                        scheduler = schedulerWithOptions
                    )
                } else {
                    todoRepository.addNewTask(
                        description,
                        parentTaskId,
                        highestPriorityAsDefault,
                        schedulerWithOptions
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
                    descriptions = description.split("\n"),
                    scheduler = schedulerWithOptions
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

    fun toggleRemoveAfterSchedule() = intent<Unit> {
        reducer {
            state.reduceData { copy(removeAfterSchedule = !removeAfterSchedule) }
        }
    }

    private suspend fun IntentBuilder.IntentContext<State<AddNewTaskState>, Content<Unit>>.navigateOutAfterCall() {
        result?.ifDataSuspend {
            state.ifDataAvailable(blockOnLoading = false) {
                val taskIdToShow = taskId ?: parentTaskId
                when {
                    taskIdToShow != null -> {
                        val destination = TodoDestination.TaskDetails(taskIdToShow)
                        navigationStore.next(
                            NavigationAction(
                                destination = destination,
                                options = NavigationOptions(destination, true)
                            )
                        )
                    }
                    else -> {
                        navigationStore.goBack()
                    }
                }
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
