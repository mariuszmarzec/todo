package com.marzec.todo.screen.tasks.model

import com.marzec.content.Content
import com.marzec.content.ifFinished
import com.marzec.delegate.DialogDelegate
import com.marzec.delegate.DialogState
import com.marzec.delegate.SearchDelegate
import com.marzec.delegate.SelectionDelegate
import com.marzec.featuretoggle.FeatureTogglesManager
import com.marzec.mvi.State
import com.marzec.mvi.Store4
import com.marzec.mvi.delegates
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.mvi.reduceContentToLoadingWithNoChanges
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceWithResult
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationOptions
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.PopEntryTarget
import com.marzec.navigation.next
import com.marzec.preferences.StateCache
import com.marzec.repository.LoginRepository
import com.marzec.screen.pickitemscreen.PickItemOptions
import com.marzec.todo.delegates.dialog.ChangePriorityDelegate
import com.marzec.todo.delegates.dialog.RemoveTaskDelegate
import com.marzec.todo.delegates.dialog.UrlDelegate
import com.marzec.todo.delegates.reorder.ReorderDelegate
import com.marzec.todo.delegates.reorder.ReorderMode
import com.marzec.todo.model.Scheduler
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.TodoDestination
import com.marzec.todo.repository.TodoRepository

class TasksStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: StateCache,
    private val todoRepository: TodoRepository,
    private val loginRepository: LoginRepository,
    private val urlDelegate: UrlDelegate,
    private val dialogDelegate: DialogDelegate<Int>,
    private val removeTaskDelegate: RemoveTaskDelegate,
    private val changePriorityDelegate: ChangePriorityDelegate,
    private val searchDelegate: SearchDelegate,
    private val scheduledOptions: PickItemOptions<Task>,
    private val selectionDelegate: SelectionDelegate<Int>,
    private val reorderDelegate: ReorderDelegate,
    private val featureTogglesManager: FeatureTogglesManager,
    private val store: Store4<State<TasksScreenState>>
) : Store4<State<TasksScreenState>> by store,
    RemoveTaskDelegate by removeTaskDelegate,
    UrlDelegate by urlDelegate,
    DialogDelegate<Int> by dialogDelegate,
    SearchDelegate by searchDelegate,
    SelectionDelegate<Int> by selectionDelegate,
    ReorderDelegate by reorderDelegate {

    override val identifier: String
        get() = cacheKey

    init {
        delegates(
            removeTaskDelegate,
            dialogDelegate,
            changePriorityDelegate,
            urlDelegate,
            searchDelegate,
            selectionDelegate,
            reorderDelegate
        )
    }

    fun loadList() = intent<Content<List<Task>>>("loadList") {
        onTrigger {
            todoRepository.observeTasks()
        }

        reducer {
            state.reduceWithResult(resultNonNull(), TasksScreenState.emptyData()) { result ->
                val taskIds = result.data.map { it.id }
                val tasks = result.data.sortedWith(
                    compareByDescending(Task::priority).thenBy(
                        Task::modifiedTime
                    )
                )
                copy(
                    tasks = tasks,
                    selected = this.selected.filter { it in taskIds }.toSet(),
                    doneButtonOnTaskList = featureTogglesManager.get("todo.doneButtonOnTaskList")
                )
            }
        }
    }

    fun onListItemClicked(id: Int) = sideEffectIntent {
        navigationStore.next(TodoDestination.TaskDetails(id))
    }

    fun addNewTask() = sideEffectIntent {
        state.asData {
            navigationStore.next(TodoDestination.AddNewTask(null, null))
        }
    }

    override suspend fun onNewState(newState: State<TasksScreenState>) {
        stateCache.set(cacheKey, newState)
    }

    fun saveReorder() = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                (reorderMode as? ReorderMode.Enabled)?.items?.let {
                    todoRepository.reorderByPriority(it)
                }
            }
        }

        reducer {
            state.reduceContentAsSideAction(resultNonNull())
        }

        sideEffect {
            state.asData {
                disableReorderMode()
            }
        }
    }

    fun logout() = intent<Content<Unit>> {
        onTrigger {
            loginRepository.logout()
        }

        sideEffect {
            resultNonNull().ifFinished {
                navigationStore.next(
                    NavigationAction(
                        destination = TodoDestination.Login,
                        NavigationOptions(
                            PopEntryTarget.ToDestination(
                                TodoDestination.Login,
                                popToInclusive = true
                            )
                        )
                    )
                )
            }
        }
    }

    fun onScheduledClick() {
        navigationStore.next(NavigationAction(TodoDestination.PickItem(scheduledOptions)))
    }

    fun showRemoveSelectedSubTasksDialog() = reducerIntent {
        state.reduceData {
            copy(
                dialog = DialogState.RemoveDialogWithCheckBox(
                    idsToRemove = selected.toList(),
                    id = DIALOG_ID_REMOVE_MULTIPLE_TASKS
                )
            )
        }
    }

    fun onScheduleSelectedClick() {
        sideEffectIntent {
            navigationStore.next(
                NavigationAction(
                    destination = TodoDestination.Schedule(
                        scheduler = null,
                        additionalOptionsAvailable = true
                    )
                ),
                requestId = REQUEST_ID_SCHEDULE_SELECTED
            )
        }
    }

    fun onScheduleSelectedRequest() = intent("onScheduleSelectedRequest") {
        onTrigger {
            navigationStore.observe(REQUEST_ID_SCHEDULE_SELECTED)
        }

        sideEffect {
            scheduleSelected(resultNonNull())
        }
    }

    private fun scheduleSelected(scheduler: Scheduler) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                tasks.filter { it.id in selected }
                    .takeIf { it.isNotEmpty() }
                    ?.let { todoRepository.schedule(it, scheduler) }
            }
        }

        reducer {
            state.reduceContentToLoadingWithNoChanges(result)
        }
    }

    fun markAsDone(id: Int) = intent {
        onTrigger {
            state.ifDataAvailable {
                todoRepository.markAsDone(id)
            }
        }
    }

    fun markAsToDo(id: Int) = intent {
        onTrigger {
            state.ifDataAvailable {
                todoRepository.markAsToDo(id)
            }
        }
    }

    companion object {
        const val DIALOG_ID_REMOVE_MULTIPLE_TASKS = "DIALOG_ID_REMOVE_MULTIPLE_TASKS"
        const val REQUEST_ID_SCHEDULE_SELECTED = 7891
    }
}
