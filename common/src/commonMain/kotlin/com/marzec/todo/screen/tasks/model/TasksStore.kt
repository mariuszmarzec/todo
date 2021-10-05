package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentNoChanges
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.extensions.emptyString
import com.marzec.todo.extensions.urlToOpen
import com.marzec.todo.model.Task
import com.marzec.todo.model.ToDoList
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.screen.taskdetails.model.TaskDetailsState
import com.marzec.todo.view.DialogState
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class TasksStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<TasksScreenState>,
    val todoRepository: TodoRepository,
    val listId: Int,
    private val openUrlHelper: OpenUrlHelper
) : Store2<State<TasksScreenState>>(
    stateCache.get(cacheKey) ?: initialState
) {

    suspend fun loadList() = intent<Content<ToDoList>> {
        onTrigger {
            todoRepository.observeList(listId)
        }

        reducer {
            state.reduceDataWithContent(resultNonNull(), TasksScreenState.EMPTY_DATA) { result ->
                copy(
                    tasks = result.data.tasks.sortedWith(
                        compareByDescending(Task::priority).thenBy(
                            Task::modifiedTime
                        )
                    )
                )
            }
        }
    }

    suspend fun onListItemClicked(id: String) = sideEffectIntent {
        navigationStore.next(Destination.TaskDetails(listId, id.toInt()))
    }

    suspend fun addNewTask() = sideEffectIntent {
        state.asData {
            navigationStore.next(Destination.AddNewTask(listId, null, null))
        }
    }

    suspend fun showRemoveDialog(id: String) = intent<Unit> {
        reducer {
            state.reduceData {
                copy(
                    dialog = DialogState.RemoveDialogWithCheckBox(idToRemove = id.toInt())
                )
            }
        }
    }

    suspend fun hideDialog() = intent<Unit> {
        reducer {
            state.reduceData {
                copy(dialog = DialogState.NoDialog)
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun isRemoveWithSubtasksChecked(
        state: State<TasksScreenState>
    ): Boolean {
        contract {
            returns(true) implies (state is State.Data)
        }
        return state is State.Data<TasksScreenState>
                && (state.data.dialog as? DialogState.RemoveDialogWithCheckBox)?.checked == true
    }

    suspend fun removeTask(idToRemove: Int) = intent<Content<Unit>> {
        onTrigger {
            if (isRemoveWithSubtasksChecked(state)) {
                todoRepository.removeTaskWithSubtasks(state.data.tasks.first { it.id == idToRemove })
            } else {
                todoRepository.removeTask(idToRemove)

            }
        }

        reducer {
            state.reduceDataWithContent(resultNonNull(), TasksScreenState.EMPTY_DATA) {
                copy(dialog = DialogState.NoDialog)
            }
        }

        sideEffect {
            hideDialog()
        }
    }

    override suspend fun onNewState(newState: State<TasksScreenState>) {
        stateCache.set(cacheKey, newState)
    }

    suspend fun moveToTop(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                val maxPriority = tasks.maxOf { it.priority }
                tasks.firstOrNull { id.toInt() == it.id }?.let { task ->
                    todoRepository.updateTask(
                        taskId = id.toInt(),
                        description = task.description,
                        parentTaskId = task.parentTaskId,
                        priority = maxPriority.inc(),
                        isToDo = task.isToDo
                    )
                }
            }
        }
        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }
    }

    suspend fun moveToBottom(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                val minPriority = tasks.minOf { it.priority }
                tasks.firstOrNull { id.toInt() == it.id }?.let { task ->
                    todoRepository.updateTask(
                        taskId = id.toInt(),
                        description = task.description,
                        parentTaskId = task.parentTaskId,
                        priority = minPriority.dec(),
                        isToDo = task.isToDo
                    )
                }
            }
        }
        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }
    }

    suspend fun openUrl(taskId: String) = sideEffectIntent {
        state.data?.tasks?.firstOrNull { task -> task.id == taskId.toInt() }
            ?.urlToOpen()
            ?.let { openUrlHelper.open(it) }
    }

    suspend fun onSearchQueryChanged(query: String) = intent<Unit> {
        reducer {
            state.reduceData {
                copy(search = query)
            }
        }
    }

    suspend fun clearSearch() = intent<Unit> {
        reducer {
            state.reduceData {
                copy(
                    search = emptyString(),
                    searchFocused = false
                )
            }
        }
    }

    suspend fun activateSearch() = intent<Unit> {
        reducer {
            state.reduceData {
                copy(
                    search = emptyString(),
                    searchFocused = true
                )
            }
        }
    }

    suspend fun onSearchFocusChanged(focused: Boolean) = intent<Unit> {
        reducer {
            state.reduceData {
                copy(searchFocused = focused)
            }
        }
    }

    suspend fun onRemoveWithSubTasksChange() = intent<Unit> {
        reducer {
            state.reduceData {
                copy(
                    dialog = dialog.asInstanceAndReturn<DialogState.RemoveDialogWithCheckBox> {
                        copy(checked = !this.checked)
                    } ?: DialogState.NoDialog
                )
            }
        }
    }
}
