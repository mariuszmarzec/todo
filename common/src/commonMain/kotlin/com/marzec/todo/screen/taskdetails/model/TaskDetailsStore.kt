package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.newMvi.Store2
import com.marzec.todo.common.CopyToClipBoardHelper
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.extensions.getMessage
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskDetailsStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: TaskDetailsState,
    private val todoRepository: TodoRepository,
    private val listId: Int,
    private val taskId: Int,
    private val copyToClipBoardHelper: CopyToClipBoardHelper,
    private val openUrlHelper: OpenUrlHelper
) : Store2<TaskDetailsState>(
    stateCache.get(cacheKey) ?: initialState
) {

    suspend fun loadDetails() = intent<Content<Task>> {
        onTrigger {
            todoRepository.observeTask(listId, taskId)
        }
        reducer {
            result?.let { result ->
                when (result) {
                    is Content.Data -> state.reduceData(result.data)
                    is Content.Error -> TaskDetailsState.Error(state.task, result.getMessage())
                    is Content.Loading -> TaskDetailsState.Loading(state.task)
                }
            } ?: state
        }
    }

    suspend fun edit() = intent<Unit> {
        sideEffect {
            state.asInstance<TaskDetailsState.Data> {
                navigationStore.next(Destination.AddNewTask(listId, taskId, task.parentTaskId))
            }
        }
    }

    suspend fun addSubTask() = intent<Unit> {
        sideEffect {
            state.asInstance<TaskDetailsState.Data> {
                navigationStore.next(Destination.AddSubTask(listId, taskId))
            }
        }
    }

    override suspend fun onNewState(newState: TaskDetailsState) {
        stateCache.set(cacheKey, newState)
    }

    suspend fun unpinSubtask(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asInstanceAndReturn<TaskDetailsState.Data, Flow<Content<Unit>>?> {
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
                copy(
                    dialog = DialogState.RemoveDialog(idToRemove = task.id)
                )
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
            todoRepository.removeTask(idToRemove)
        }

        reducer {
            when (val result = resultNonNull()) {
                is Content.Data -> {
                    state.reduceData {
                        copy(dialog = DialogState.NoDialog)
                    }
                }
                is Content.Error -> TaskDetailsState.Error(state.task, result.getMessage())
                is Content.Loading -> TaskDetailsState.Loading(state.task)
            }
        }

        sideEffect {
            hideDialog()
            resultNonNull().asInstance<Content.Data<Unit>> {
                if (idToRemove == taskId) {
//                    navigationStore.goBack()
                }
            }
        }
    }

    suspend fun moveToTop(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asInstanceAndReturn<TaskDetailsState.Data, Flow<Content<Unit>>?> {
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
            when (val result = resultNonNull()) {
                is Content.Data -> state
                is Content.Error -> TaskDetailsState.Error(
                    state.task,
                    result.exception.message.orEmpty()
                )
                is Content.Loading -> TaskDetailsState.Loading(state.task)
            }
        }
    }

    suspend fun moveToBottom(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asInstanceAndReturn<TaskDetailsState.Data, Flow<Content<Unit>>?> {
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
            when (val result = resultNonNull()) {
                is Content.Data -> state
                is Content.Error -> TaskDetailsState.Error(
                    state.task,
                    result.exception.message.orEmpty()
                )
                is Content.Loading -> TaskDetailsState.Loading(state.task)
            }
        }
    }

    suspend fun copyDescription() = sideEffectIntent {
        state.asInstance<TaskDetailsState.Data> {
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
}

private fun TaskDetailsState.reduceData(data: Task): TaskDetailsState =
    this.asInstanceAndReturn<
            TaskDetailsState.Data,
            TaskDetailsState
            > { copy(task = data) } ?: TaskDetailsState.Data(
        task = data,
        dialog = DialogState.NoDialog
    )

private fun TaskDetailsState.reduceData(
    reducer: TaskDetailsState.Data.() -> TaskDetailsState.Data
): TaskDetailsState =
    when (this) {
        is TaskDetailsState.Data -> this.reducer()
        is TaskDetailsState.Loading -> TaskDetailsState.Loading(task)
        is TaskDetailsState.Error -> TaskDetailsState.Error(task, message)
    }