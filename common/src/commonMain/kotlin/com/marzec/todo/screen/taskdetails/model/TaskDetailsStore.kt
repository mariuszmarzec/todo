package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class TaskDetailsStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<TaskDetailsState>,
    private val todoRepository: TodoRepository,
    private val listId: Int,
    private val taskId: Int,
    private val copyToClipBoardHelper: CopyToClipBoardHelper,
    private val openUrlHelper: OpenUrlHelper
) : Store2<State<TaskDetailsState>>(
    stateCache.get(cacheKey) ?: initialState
) {

    suspend fun loadDetails() = intent<Content<Task>> {
        onTrigger(isCancellableFlowTrigger = true) {
            todoRepository.observeTask(listId, taskId)
        }
        reducer {
            state.reduceDataWithContent(resultNonNull()) { result ->
                TaskDetailsState(result, DialogState.NoDialog)
            }
        }
    }

    suspend fun edit() = intent<Unit> {
        sideEffect {
            state.ifDataAvailable {
                navigationStore.next(Destination.AddNewTask(listId, taskId, task.parentTaskId))
            }
        }
    }

    suspend fun addSubTask() = intent<Unit> {
        sideEffect {
            state.ifDataAvailable {
                navigationStore.next(Destination.AddSubTask(listId, taskId))
            }
        }
    }

    override suspend fun onNewState(newState: State<TaskDetailsState>) {
        stateCache.set(cacheKey, newState)
    }

    suspend fun unpinSubtask(id: String) = intent<Content<Unit>> {
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

    suspend fun goToSubtaskDetails(id: String) = sideEffectIntent {
        navigationStore.next(Destination.TaskDetails(listId, id.toInt()))
    }

    suspend fun showRemoveTaskDialog() = intent<Unit> {
        reducer {
            state.reduceData {
                copy(dialog = DialogState.RemoveDialogWithCheckBox(idToRemove = task.id))
            }
        }
    }

    suspend fun showRemoveSubTaskDialog(subtaskId: String) = intent<Unit> {
        reducer {
            state.reduceData {
                copy(
                    dialog = DialogState.RemoveDialog(idToRemove = subtaskId.toInt())
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

    suspend fun removeTask(idToRemove: Int) = intent<Content<Unit>> {
        onTrigger {
            if (isRemoveWithSubtasksChecked(state)) {
                todoRepository.removeTaskWithSubtasks(state.data.task)
            } else {
                todoRepository.removeTask(idToRemove)

            }.cancelFlowsIf { it is Content.Data && idToRemove == taskId }
        }

        reducer {
            state.reduceContentAsSideAction(resultNonNull())
        }

        sideEffect {
            resultNonNull().asInstance<Content.Data<Unit>> {
                if (idToRemove == taskId) {
                    navigationStore.goBack()
                }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun isRemoveWithSubtasksChecked(
        state: State<TaskDetailsState>
    ): Boolean {
        contract {
            returns(true) implies (state is State.Data)
        }
        return state is State.Data
                && (state.data.dialog as? DialogState.RemoveDialogWithCheckBox)?.checked == true
    }

    suspend fun moveToTop(id: String) = intent<Content<Unit>> {
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

    suspend fun moveToBottom(id: String) = intent<Content<Unit>> {
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

    suspend fun copyDescription() = sideEffectIntent {
        state.ifDataAvailable {
            copyToClipBoardHelper.copy(task.description)
        }
    }

    suspend fun openUrls(urls: List<String>) = sideEffectIntent {
        if (urls.size == 1) {
            openUrl(urls.first())
        } else {
            showSelectUrlDialog(urls)
        }
    }

    suspend fun openUrl(url: String) = sideEffectIntent {
        openUrlHelper.open(url)
    }

    private suspend fun showSelectUrlDialog(urls: List<String>) = intent<Unit> {
        reducer {
            state.reduceData {
                copy(dialog = DialogState.SelectOptionsDialog(urls))
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

    suspend fun explodeIntoTasks(tasks: List<String>) = intent<Content<Unit>> {
        onTrigger { todoRepository.addNewTasks(listId, false, taskId, tasks) }
    }
}
