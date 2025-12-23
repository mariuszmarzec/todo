package com.marzec.todo.screen.addnewtask.model

import com.marzec.content.Content
import com.marzec.content.ifDataSuspend
import com.marzec.model.toNullableUpdate
import com.marzec.model.toUpdate
import com.marzec.mvi.IntentContext
import com.marzec.mvi.State
import com.marzec.mvi.Store4Impl
import com.marzec.mvi.reduceContentToLoadingWithNoChanges
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceWithResult
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationOptions
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.PopEntryTarget
import com.marzec.preferences.StateCache
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task
import com.marzec.todo.model.UpdateTask
import com.marzec.todo.navigation.TodoDestination
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull

class AddNewTaskStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: StateCache,
    private val initialState: State<AddNewTaskState>,
    private val todoRepository: TodoRepository,
) : Store4Impl<State<AddNewTaskState>>(
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
                        state.reduceWithResult(
                            result = resultNonNull(),
                            defaultData = AddNewTaskState.default(
                                taskId = 0,
                                parentTaskId = null
                            )
                        ) { result ->
                                copy(
                                taskId = result.data.id,
                                task = result.data,
                                parentTaskId = result.data.parentTaskId,
                                description = result.data.description,
                                priority = result.data.priority,
                                isToDo = result.data.isToDo,
                                scheduler = result.data.scheduler,
                                highestPriorityAsDefault = result.data.scheduler?.highestPriorityAsDefault
                                    ?: Scheduler.HIGHEST_PRIORITY_AS_DEFAULT,
                                removeAfterSchedule = (result.data.scheduler as? Scheduler.OneShot)?.removeScheduled
                                    ?: Scheduler.REMOVE_SCHEDULED,
                                showNotification = result.data.scheduler?.showNotification
                                    ?: Scheduler.SHOW_NOTIFICATION
                            )
                        }
                    } ?: state
                }
            }
        }

    fun onSchedulerRequest() = intent("onSchedulerRequest") {
        onTrigger {
            navigationStore.observe<Scheduler>(REQUEST_KEY_SCHEDULER)?.filterNotNull()
        }

        reducer {
            val scheduler = resultNonNull()
            state.reduceData {
                copy(
                    scheduler = scheduler,
                    removeAfterSchedule = scheduler is Scheduler.OneShot
                )
            }
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
                if (task != null) {
                    todoRepository.updateTask(
                        taskId = task.id,
                        UpdateTask(
                            description = description.toUpdate(task.description),
                            parentTaskId = parentTaskId.toNullableUpdate(task.parentTaskId),
                            priority = priority.toUpdate(task.priority),
                            isToDo = isToDo.toUpdate(task.isToDo),
                            scheduler = schedulerWithOptions.toNullableUpdate(task.scheduler)
                        )
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
            state.reduceContentToLoadingWithNoChanges(resultNonNull())
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
                    descriptions = description.split("\n").let {
                        if (highestPriorityAsDefault) {
                            it.reversed()
                        } else {
                            it
                        }
                    },
                    scheduler = schedulerWithOptions
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

    fun toggleShowNotification() = reducerIntent {
        state.reduceData {
            copy(showNotification = !showNotification)
        }
    }

    private suspend fun IntentContext<State<AddNewTaskState>, Content<Unit>>.navigateOutAfterCall() {
        result?.ifDataSuspend {
            state.ifDataAvailable(blockOnLoading = false) {
                val taskIdToShow = taskId ?: parentTaskId
                when {
                    taskIdToShow != null -> {
                        val destination = TodoDestination.TaskDetails(taskIdToShow)
                        navigationStore.next(
                            NavigationAction(
                                destination = destination,
                                options = NavigationOptions(
                                    PopEntryTarget.ToDestination(
                                        popTo = destination,
                                        popToInclusive = true
                                    )
                                )
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

    fun onScheduleButtonClick() = sideEffectIntent {
        state.ifDataAvailable {
            navigationStore.next(
                NavigationAction(TodoDestination.Schedule(scheduler)),
                requestId = REQUEST_KEY_SCHEDULER
            )
        }
    }

    fun onRemoveSchedulerButtonClick() = intent<Unit> {
        reducer {
            state.reduceData {
                copy(scheduler = null)
            }
        }
    }
}

const val REQUEST_KEY_SCHEDULER = 1