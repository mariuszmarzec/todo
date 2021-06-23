package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.newMvi.Store2
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
import com.marzec.todo.screen.tasks.model.RemoveDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TaskDetailsStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: TaskDetailsState,
    private val todoRepository: TodoRepository,
    private val listId: Int,
    private val taskId: Int
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
                    flow {
                        emit(Content.Loading())
                        emit(
                            todoRepository.updateTask(
                                taskId = id.toInt(),
                                description = task.description,
                                parentTaskId = null,
                                priority = task.priority,
                                isToDo = task.isToDo
                            )
                        )
                    }
                }
            }
        }

        sideEffect {
            result?.asInstance<Content.Data<*>> {
                loadDetails()
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
                    removeTaskDialog = removeTaskDialog.copy(
                        visible = true,
                        idToRemove = task.id
                    )
                )
            }
        }
    }

    suspend fun showRemoveSubTaskDialog(subtaskId: String) = intent<Unit> {
        reducer {
            state.reduceData {
                copy(
                    removeTaskDialog = removeTaskDialog.copy(
                        visible = true,
                        idToRemove = subtaskId.toInt()
                    )
                )
            }
        }
    }

    suspend fun hideRemoveTaskDialog() = intent<Unit> {
        reducer {
            state.reduceData {
                copy(
                    removeTaskDialog = removeTaskDialog.copy(
                        visible = false
                    )
                )
            }
        }
    }

    suspend fun removeTask() = intent<Content<Unit>> {
        onTrigger {
            state.asInstanceAndReturn<TaskDetailsState.Data, Flow<Content<Unit>>> {
                flow {
                    emit(
                        todoRepository.removeTask(removeTaskDialog.idToRemove)
                    )
                }
            }
        }

        reducer {
            when (val result = resultNonNull()) {
                is Content.Data -> {
                    state.reduceData {
                        copy(
                            removeTaskDialog = removeTaskDialog.copy(
                                visible = false
                            )
                        )
                    }
                }
                is Content.Error -> TaskDetailsState.Error(state.task, result.getMessage())
                is Content.Loading -> TaskDetailsState.Loading(state.task)
            }
        }

        sideEffect {
            hideRemoveTaskDialog()
            state.asInstance<TaskDetailsState.Data> {
                if (removeTaskDialog.idToRemove == task.id) {
                    navigationStore.goBack()
                } else {
                    loadDetails()
                }
            }
        }
    }

    suspend fun moveToTop(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asInstanceAndReturn<TaskDetailsState.Data, Flow<Content<Unit>>?> {
                val maxPriority = task.subTasks.maxOf { it.priority }
                task.subTasks.firstOrNull { id.toInt() == it.id }?.let { task ->
                    flow {
                        emit(Content.Loading())
                        emit(
                            todoRepository.updateTask(
                                taskId = id.toInt(),
                                description = task.description,
                                parentTaskId = task.parentTaskId,
                                priority = maxPriority.inc(),
                                isToDo = task.isToDo
                            )
                        )
                    }
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
        sideEffect {
            loadDetails()
        }
    }

    suspend fun moveToBottom(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asInstanceAndReturn<TaskDetailsState.Data, Flow<Content<Unit>>?> {
                val minPriority = task.subTasks.minOf { it.priority }
                task.subTasks.firstOrNull { id.toInt() == it.id }?.let { task ->
                    flow {
                        emit(Content.Loading())
                        emit(
                            todoRepository.updateTask(
                                taskId = id.toInt(),
                                description = task.description,
                                parentTaskId = task.parentTaskId,
                                priority = minPriority.dec(),
                                isToDo = task.isToDo
                            )
                        )
                    }
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
        sideEffect {
            loadDetails()
        }
    }
}

private fun TaskDetailsState.reduceData(data: Task): TaskDetailsState =
    this.asInstanceAndReturn<
            TaskDetailsState.Data,
            TaskDetailsState
            > { copy(task = data) } ?: TaskDetailsState.Data(
        task = data,
        removeTaskDialog = RemoveDialog()
    )

private fun TaskDetailsState.reduceData(
    reducer: TaskDetailsState.Data.() -> TaskDetailsState.Data
): TaskDetailsState =
    when (this) {
        is TaskDetailsState.Data -> this.reducer()
        is TaskDetailsState.Loading -> TaskDetailsState.Loading(task)
        is TaskDetailsState.Error -> TaskDetailsState.Error(task, message)
    }