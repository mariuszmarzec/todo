package com.marzec.todo.screen.taskdetails.model

import com.marzec.content.Content
import com.marzec.delegate.delegates
import com.marzec.extensions.asInstance
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.mvi.reduceContentNoChanges
import com.marzec.mvi.reduceDataWithContent
import com.marzec.navigation.NavigationStore
import com.marzec.navigation.next
import com.marzec.preferences.Preferences
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.delegates.dialog.ChangePriorityDelegate
import com.marzec.delegate.DialogDelegate
import com.marzec.todo.delegates.dialog.RemoveTaskDelegate
import com.marzec.delegate.SearchDelegate
import com.marzec.delegate.SelectionDelegate
import com.marzec.todo.delegates.dialog.UrlDelegate
import com.marzec.todo.delegates.dialog.removeTaskOnTrigger
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.TodoDestination
import com.marzec.todo.repository.TodoRepository
import com.marzec.delegate.DialogState
import com.marzec.view.SearchState
import kotlinx.coroutines.CoroutineScope

class TaskDetailsStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<TaskDetailsState>,
    private val todoRepository: TodoRepository,
    private val taskId: Int,
    private val copyToClipBoardHelper: CopyToClipBoardHelper,
    private val dialogDelegate: DialogDelegate,
    private val removeTaskDelegate: RemoveTaskDelegate,
    private val urlDelegate: UrlDelegate,
    private val changePriorityDelegate: ChangePriorityDelegate,
    private val selectionDelegate: SelectionDelegate<Int>,
    private val searchDelegate: SearchDelegate
) : Store3<State<TaskDetailsState>>(
    scope, stateCache.get(cacheKey) ?: initialState
), RemoveTaskDelegate by removeTaskDelegate,
    UrlDelegate by urlDelegate,
    DialogDelegate by dialogDelegate,
    SelectionDelegate<Int> by selectionDelegate,
    SearchDelegate by searchDelegate {

    override val identifier: String
        get() = cacheKey

    init {
        delegates(
            removeTaskDelegate,
            urlDelegate,
            dialogDelegate,
            changePriorityDelegate,
            selectionDelegate,
            searchDelegate
        )
    }

    fun loadDetails() = intent<Content<Task>>("loadDetails") {
        onTrigger {
            todoRepository.observeTask(taskId)
        }
        reducer {
            state.reduceDataWithContent(resultNonNull()) { result ->
                val taskIds = result.subTasks.map { it.id }.toSet()
                TaskDetailsState(
                    task = result,
                    dialog = DialogState.NoDialog,
                    selected = this?.selected?.filter { it in taskIds }?.toSet().orEmpty(),
                    search = SearchState(
                        value = "",
                        focused = false
                    )
                )
            }
        }
    }

    fun edit() = intent<Unit> {
        sideEffect {
            state.ifDataAvailable {
                navigationStore.next(TodoDestination.AddNewTask(taskId, task.parentTaskId))
            }
        }
    }

    fun addSubTask() = intent<Unit> {
        sideEffect {
            state.ifDataAvailable {
                navigationStore.next(TodoDestination.AddSubTask(taskId))
            }
        }
    }

    override suspend fun onNewState(newState: State<TaskDetailsState>) {
        stateCache.set(cacheKey, newState)
    }

    fun unpinSubtask(id: Int) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                task.subTasks.firstOrNull { id == it.id }?.let { task ->
                    todoRepository.updateTask(
                        taskId = id,
                        description = task.description,
                        parentTaskId = null,
                        priority = task.priority,
                        isToDo = task.isToDo,
                        scheduler = task.scheduler
                    )
                }
            }
        }
    }

    fun goToSubtaskDetails(id: Int) = sideEffect {
        navigationStore.next(TodoDestination.TaskDetails(id))
    }

    fun showRemoveTaskDialog() =
        removeTaskDelegate.onRemoveButtonClick(taskId)

    fun showRemoveSubTaskDialog(subtaskId: Int) =
        removeTaskDelegate.onRemoveButtonClick(subtaskId)

    override fun removeTask(idsToRemove: List<Int>) = sideEffect {
        closeDialog()

        intent<Content<Unit>> {
            removeTaskOnTrigger(todoRepository, idsToRemove)

            sideEffect {
                resultNonNull().asInstance<Content.Data<Unit>> {
                    if (idsToRemove.first() == taskId) {
                        navigationStore.goBack()
                    }
                }
            }
        }
    }

    fun moveToTop(id: Int) = sideEffect {
        state.ifDataAvailable {
            changePriorityDelegate.changePriority(
                id = id,
                newPriority = task.subTasks.maxOf { it.priority }.inc()
            )
        }
    }

    fun moveToBottom(id: Int) = sideEffect {
        state.ifDataAvailable {
            changePriorityDelegate.changePriority(
                id = id,
                newPriority = task.subTasks.minOf { it.priority }.dec()
            )
        }
    }

    fun copyDescription() = sideEffect {
        state.ifDataAvailable {
            copyToClipBoardHelper.copy(task.description)
        }
    }

    fun explodeIntoTasks(tasks: List<String>) = intent {
        onTrigger { todoRepository.addNewTasks(false, taskId, tasks, null) }
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

    fun unpinSubtasks() = sideEffect {
        state.ifDataAvailable {
            task.subTasks.forEach {
                unpinSubtask(it.id)
            }
        }
    }

    fun showRemoveSelectedSubTasksDialog() = sideEffect {
        state.ifDataAvailable {
            onRemoveButtonClick(selected.toList())
        }
    }

    fun removeDoneTasks() = sideEffect {
        state.ifDataAvailable {
            val ids = task.subTasks.filterNot { it.isToDo }.map { it.id }
            onRemoveButtonClick(ids)
        }
    }

    fun markSelectedAsTodo() = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                todoRepository.markAsToDo(selected.toList())
            }
        }

        reducer { state.reduceContentNoChanges(resultNonNull()) }
    }

    fun markSelectedAsDone() = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                todoRepository.markAsDone(selected.toList())
            }
        }

        reducer { state.reduceContentNoChanges(resultNonNull()) }
    }

    fun copyTask() = intent() {
        onTrigger {
            todoRepository.copyTask(taskId)
        }

        sideEffect {
            navigationStore.goBack()
        }
    }
}
