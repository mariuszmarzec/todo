package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.delegates.dialog.DialogDelegate
import com.marzec.todo.delegates.dialog.RemoveTaskDelegate
import com.marzec.todo.delegates.dialog.UrlDelegate
import com.marzec.todo.delegates.dialog.UrlDelegateImpl
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.delegates
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState

class TaskDetailsStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<TaskDetailsState>,
    private val todoRepository: TodoRepository,
    private val listId: Int,
    private val taskId: Int,
    private val copyToClipBoardHelper: CopyToClipBoardHelper,
    private val dialogDelegate: DialogDelegate,
    private val removeTaskDelegate: RemoveTaskDelegate,
    private val urlDelegate: UrlDelegate<TaskDetailsState>
) : Store2<State<TaskDetailsState>>(
    stateCache.get(cacheKey) ?: initialState
), RemoveTaskDelegate by removeTaskDelegate,
    DialogDelegate by dialogDelegate {

    init {
        delegates(
            removeTaskDelegate,
            urlDelegate,
            dialogDelegate
        )
    }

    fun loadDetails() = intent<Content<Task>> {
        onTrigger(isCancellableFlowTrigger = true) {
            todoRepository.observeTask(listId, taskId)
        }
        reducer {
            state.reduceDataWithContent(resultNonNull()) { result ->
                TaskDetailsState(result, DialogState.NoDialog)
            }
        }
    }

    fun edit() = intent<Unit> {
        sideEffect {
            state.ifDataAvailable {
                navigationStore.next(Destination.AddNewTask(listId, taskId, task.parentTaskId))
            }
        }
    }

    fun addSubTask() = intent<Unit> {
        sideEffect {
            state.ifDataAvailable {
                navigationStore.next(Destination.AddSubTask(listId, taskId))
            }
        }
    }

    override suspend fun onNewState(newState: State<TaskDetailsState>) {
        stateCache.set(cacheKey, newState)
    }

    fun unpinSubtask(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                task.subTasks.firstOrNull { id.toInt() == it.id }?.let { task ->
                    todoRepository.updateTask(
                        taskId = id.toInt(),
                        description = task.description,
                        parentTaskId = null,
                        priority = task.priority,
                        isToDo = task.isToDo
                    )
                }
            }
        }
    }

    fun goToSubtaskDetails(id: String) = sideEffectIntent {
        navigationStore.next(Destination.TaskDetails(listId, id.toInt()))
    }

    fun showRemoveTaskDialog() =
        removeTaskDelegate.onRemoveButtonClick(taskId.toString())

    fun showRemoveSubTaskDialog(subtaskId: String) =
        removeTaskDelegate.onRemoveButtonClick(subtaskId)

    fun hideDialog() = dialogDelegate.closeDialog()

    override fun removeTask(idToRemove: Int) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                if ((dialog as? DialogState.RemoveDialogWithCheckBox)?.checked == true) {
                    todoRepository.removeTaskWithSubtasks(taskById(idToRemove))
                } else {
                    todoRepository.removeTask(idToRemove)
                }
            }
        }

        reducer {
            state.reduceContentAsSideAction(resultNonNull()) {
                copyWithDialog(dialog = DialogState.NoDialog)
            }
        }

        sideEffect {
            resultNonNull().asInstance<Content.Data<Unit>> {
                if (idToRemove == taskId) {
                    navigationStore.goBack()
                }
            }
        }
    }

    fun moveToTop(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                val maxPriority = task.subTasks.maxOf { it.priority }
                task.subTasks.firstOrNull { id.toInt() == it.id }?.let { task ->
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
            state.reduceContentAsSideAction(resultNonNull())
        }
    }

    fun moveToBottom(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                val minPriority = task.subTasks.minOf { it.priority }
                task.subTasks.firstOrNull { id.toInt() == it.id }?.let { task ->
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
            state.reduceContentAsSideAction(resultNonNull())
        }
    }

    fun copyDescription() = sideEffectIntent {
        state.ifDataAvailable {
            copyToClipBoardHelper.copy(task.description)
        }
    }

    fun openUrls(urls: List<String>) = urlDelegate.openUrls(urls)

    fun openUrl(url: String) = urlDelegate.openUrl(url)

    fun explodeIntoTasks(tasks: List<String>) = intent<Content<Unit>> {
        onTrigger { todoRepository.addNewTasks(listId, false, taskId, tasks) }
    }
}
