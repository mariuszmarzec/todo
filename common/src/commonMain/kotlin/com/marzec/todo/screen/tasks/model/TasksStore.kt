package com.marzec.todo.screen.tasks.model

import com.marzec.content.Content
import com.marzec.content.asContentFlow
import com.marzec.content.ifFinished
import com.marzec.delegate.SearchDelegate
import com.marzec.delegate.delegates
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.mvi.reduceDataWithContent
import com.marzec.navigation.NavigationAction
import com.marzec.navigation.NavigationOptions
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.next
import com.marzec.preferences.StateCache
import com.marzec.repository.LoginRepository
import com.marzec.todo.delegates.dialog.ChangePriorityDelegate
import com.marzec.delegate.DialogDelegate
import com.marzec.delegate.DialogState
import com.marzec.delegate.ScrollDelegate
import com.marzec.delegate.SelectionDelegate
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.mvi.reduceContentToLoadingWithNoChanges
import com.marzec.mvi.reduceData
import com.marzec.navigation.PopEntryTarget
import com.marzec.screen.pickitemscreen.PickItemOptions
import com.marzec.todo.delegates.dialog.RemoveTaskDelegate
import com.marzec.todo.delegates.dialog.UrlDelegate
import com.marzec.todo.delegates.reorder.ReorderDelegate
import com.marzec.todo.delegates.reorder.ReorderMode
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.TodoDestination
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf

class TasksStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: StateCache,
    initialState: State<TasksScreenState>,
    private val todoRepository: TodoRepository,
    private val loginRepository: LoginRepository,
    private val urlDelegate: UrlDelegate,
    private val dialogDelegate: DialogDelegate<Int>,
    private val removeTaskDelegate: RemoveTaskDelegate,
    private val changePriorityDelegate: ChangePriorityDelegate,
    private val searchDelegate: SearchDelegate,
    private val scrollDelegate: ScrollDelegate,
    private val scheduledOptions: PickItemOptions<Task>,
    private val selectionDelegate: SelectionDelegate<Int>,
    private val reorderDelegate: ReorderDelegate
) : Store3<State<TasksScreenState>>(
    scope,
    stateCache.get(cacheKey) ?: initialState
), RemoveTaskDelegate by removeTaskDelegate,
    UrlDelegate by urlDelegate,
    DialogDelegate<Int> by dialogDelegate,
    SearchDelegate by searchDelegate,
    ScrollDelegate by scrollDelegate,
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
            scrollDelegate,
            selectionDelegate,
            reorderDelegate
        )
    }

    fun loadList() = intent<Content<List<Task>>>("loadList") {
        onTrigger {
            todoRepository.observeTasks()
        }

        reducer {
            state.reduceDataWithContent(resultNonNull(), TasksScreenState.emptyData()) { result ->
                val taskIds = result.data.map { it.id }
                val tasks = result.data.sortedWith(
                    compareByDescending(Task::priority).thenBy(
                        Task::modifiedTime
                    )
                )
                copy(
                    tasks = tasks,
                    selected = this.selected.filter { it in taskIds }.toSet()
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

    companion object {
        const val DIALOG_ID_REMOVE_MULTIPLE_TASKS = "DIALOG_ID_REMOVE_MULTIPLE_TASKS"
    }
}
