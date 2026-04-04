package com.marzec.todo.screen.addnewtask.model

import com.marzec.content.Content
import com.marzec.content.combineContentsFlows
import com.marzec.content.ifDataSuspend
import com.marzec.featuretoggle.FeatureTogglesManager
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
import com.marzec.repository.LoginRepository
import com.marzec.screen.featuretoggle.FeatureToggleDetails
import com.marzec.screen.pickitemscreen.PickItemOptions
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task
import com.marzec.todo.model.TaskShare
import com.marzec.todo.model.UpdateTask
import com.marzec.todo.navigation.TodoDestination
import com.marzec.todo.repository.TodoRepository
import kotlin.math.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.datetime.LocalDateTime

class AddNewTaskStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: StateCache,
    private val initialState: State<AddNewTaskState>,
    private val todoRepository: TodoRepository,
    private val loginRepository: LoginRepository,
    private val featureTogglesManager: FeatureTogglesManager,
    private val usersPickOptions: PickItemOptions<User>
) : Store4Impl<State<AddNewTaskState>>(
    scope, stateCache.get(cacheKey) ?: initialState
) {

    fun initialLoad() = (stateCache.get(cacheKey) ?: initialState)
        .asInstance<State.Loading<AddNewTaskState>> {
            intent<Content<Triple<Task, User, List<User>>>>("load") {
                onTrigger {
                    state.ifDataAvailable(blockOnLoading = false) {
                        taskId?.let {
                            combineContentsFlows(
                                todoRepository.observeTask(it),
                                loginRepository.observeCurrentUser(),
                                todoRepository.getUsers()
                            ) { task, user, users ->
                                Triple(task, user, users)
                            }
                        }
                    }
                }
                reducer {
                    result?.let {
                        state.reduceWithResult(
                            result = resultNonNull(),
                            defaultData = AddNewTaskState.default(
                                taskId = 0,
                                parentTaskId = null,
                                isTaskSharingEnabled = featureTogglesManager.get("todo.taskSharing"),
                            )
                        ) { result ->
                            val (task, user, users) = result.data
                            copy(
                                taskId = task.id,
                                task = task,
                                parentTaskId = task.parentTaskId,
                                description = task.description,
                                priority = task.priority,
                                isToDo = task.isToDo,
                                scheduler = task.scheduler,
                                expirationDate = task.expirationDate,
                                shares = task.shares,
                                highestPriorityAsDefault = task.scheduler?.highestPriorityAsDefault
                                    ?: Scheduler.HIGHEST_PRIORITY_AS_DEFAULT,
                                removeAfterSchedule = (task.scheduler as? Scheduler.OneShot)?.removeScheduled
                                    ?: Scheduler.REMOVE_SCHEDULED,
                                showNotification = task.scheduler?.showNotification
                                    ?: Scheduler.SHOW_NOTIFICATION,
                                isTaskSharingEnabled = featureTogglesManager.get("todo.taskSharing"),
                                ownedTask = task.ownerId == user.id,
                                isEditor = task.ownerId == user.id || task.shares.firstOrNull { it.userId.toInt() == user.id }?.permission == "EDITOR_AND_VIEWER",
                                users = users,
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

    fun onExpirationDateRequest() = intent("onExpirationDateRequest") {
        onTrigger {
            navigationStore.observe<LocalDateTime>(REQUEST_KEY_EXPIRATION_DATE)?.filterNotNull()
        }

        reducer {
            val expirationDate = resultNonNull()
            state.reduceData {
                copy(
                    expirationDate = expirationDate
                )
            }
        }
    }

    fun onUsersRequest() = intent("onUsersRequest") {
        onTrigger {
            navigationStore.observe<List<String>>(REQUEST_KEY_USERS)?.filterNotNull()
        }

        reducer {
            val userIds = resultNonNull().toSet()
            state.reduceData {
                copy(
                    shares = userIds.map { TaskShare(it, "EDITOR_AND_VIEWER") }
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
                            expirationDate = expirationDate.toNullableUpdate(task.expirationDate),
                            shares = shares.toUpdate(task.shares)
                        )
                    )
                } else {
                    todoRepository.addNewTask(
                        description = description,
                        parentTaskId = parentTaskId,
                        highestPriorityAsDefault = highestPriorityAsDefault,
                        scheduler = schedulerWithOptions,
                        expirationDate = expirationDate,
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
                    scheduler = schedulerWithOptions,
                    expirationDate = expirationDate
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

    fun onExpirationDateButtonClick() = sideEffectIntent {
        state.ifDataAvailable {
            navigationStore.next(
                NavigationAction(TodoDestination.DatePicker(expirationDate ?: com.marzec.time.currentTime())),
                requestId = REQUEST_KEY_EXPIRATION_DATE
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

    fun onRemoveExpirationDateButtonClick() = intent<Unit> {
        reducer {
            state.reduceData {
                copy(expirationDate = null)
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
                    schedulerWithOptions,
                    expirationDate
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
const val REQUEST_KEY_EXPIRATION_DATE = 3
