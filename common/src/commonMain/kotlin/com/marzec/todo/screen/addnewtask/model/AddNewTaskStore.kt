package com.marzec.todo.screen.addnewtask.model

import com.marzec.content.Content
import com.marzec.content.ifDataSuspend
import com.marzec.model.User
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
import com.marzec.screen.pickitemscreen.PickItemOptions
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task
import com.marzec.todo.model.TaskShare
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
    private val usersPickOptions: PickItemOptions<User>
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
                                shares = result.data.shares,
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

    fun onUsersRequest() = intent("onUsersRequest") {
        onTrigger {
            navigationStore.observe<Set<String>>(REQUEST_KEY_USERS)?.filterNotNull()
        }

        reducer {
            val userIds = resultNonNull()
            state.reduceData {
                copy(
                    shares = userIds.map { TaskShare(it, "EDIT") }
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
                            scheduler = schedulerWithOptions.toNullableUpdate(task.scheduler),
                            shares = shares.toUpdate(task.shares)
                        )
                    )
                } else {
                    todoRepository.addNewTask(
                        description = description,
                        parentTaskId = parentTaskId,
                        highestPriorityAsDefault = highestPriorityAsDefault,
                        scheduler = schedulerWithOptions,
                        shares = shares
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

    fun onUsersButtonClick() = sideEffectIntent {
        state.ifDataAvailable {
            navigationStore.next(
                NavigationAction(TodoDestination.PickItem(usersPickOptions.copy(selected = shares.map { it.userId }.toSet()))),
                requestId = REQUEST_KEY_USERS
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

    fun onRemoveShareButtonClick(userId: String) = intent<Unit> {
        reducer {
            state.reduceData {
                copy(shares = shares.filter { it.userId != userId })
            }
        }
    }

    fun createTree() = intent<Content<Unit>>("createTree") {
        onTrigger {
            state.ifDataAvailable {
                todoRepository.createTaskTree(
                    description,
                    parentTaskId,
                    highestPriorityAsDefault,
                    schedulerWithOptions
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

    fun leaveShare() = intent<Content<Unit>>("leaveShare") {
        onTrigger {
            state.ifDataAvailable {
                taskId?.let { todoRepository.leaveShare(it) }
            }
        }

        reducer {
            state.reduceContentToLoadingWithNoChanges(resultNonNull())
        }

        sideEffect {
            navigationStore.goBack()
        }
    }
}

const val REQUEST_KEY_SCHEDULER = 1
const val REQUEST_KEY_USERS = 2
