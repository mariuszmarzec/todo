package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.newMvi.Store2
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.extensions.getMessage
import com.marzec.todo.model.Task
import com.marzec.todo.model.ToDoList
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TasksStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: TasksScreenState,
    val todoRepository: TodoRepository,
    val listId: Int
) : Store2<TasksScreenState>(
    stateCache.get(cacheKey) ?: initialState
) {

    suspend fun loadList() = intent<Content<ToDoList>> {
        onTrigger {
            todoRepository.observeLists(listId)
        }

        reducer {
            when (val result = resultNonNull()) {
                is Content.Data -> state.reduceData(result.data)
                is Content.Error -> TasksScreenState.Error(state.tasks, result.exception.message.orEmpty())
                is Content.Loading -> TasksScreenState.Loading(state.tasks)
            }
        }
    }

    suspend fun onListItemClicked(id: String) = sideEffectIntent {
        navigationStore.next(Destination.TaskDetails(listId, id.toInt()))
    }

    suspend fun addNewTask() = sideEffectIntent {
        state.asInstance<TasksScreenState.Data> {
            navigationStore.next(Destination.AddNewTask(listId, null, null))
        }
    }

    suspend fun showRemoveDialog(id: String) = intent<Unit> {
        reducer {
            state.reduceData {
                copy(
                    removeTaskDialog = removeTaskDialog.copy(
                        visible = true,
                        idToRemove = id.toInt()
                    )
                )
            }
        }
    }

    suspend fun hideRemoveDialog() = intent<Unit> {
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


    suspend fun removeTask(id: Int) = intent<Content<Unit>> {
        onTrigger {
            flow {
                emit(Content.Loading())
                emit(todoRepository.removeTask(id))
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
                is Content.Error -> TasksScreenState.Error(state.tasks, result.getMessage())
                is Content.Loading -> TasksScreenState.Loading(state.tasks)
            }
        }

        sideEffect {
            hideRemoveDialog()
            loadList()
        }
    }

    override suspend fun onNewState(newState: TasksScreenState) {
        stateCache.set(cacheKey, newState)
    }

    suspend fun moveToTop(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asInstanceAndReturn<TasksScreenState.Data, Flow<Content<Unit>>?> {
                val maxPriority = tasks.maxOf { it.priority }
                tasks.firstOrNull { id.toInt() == it.id }?.let { task ->
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
                is Content.Error -> TasksScreenState.Error(
                    state.tasks,
                    result.exception.message.orEmpty()
                )
                is Content.Loading -> TasksScreenState.Loading(state.tasks)
            }
        }
        sideEffect {
            loadList()
        }
    }

    suspend fun moveToBottom(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asInstanceAndReturn<TasksScreenState.Data, Flow<Content<Unit>>?> {
                val minPriority = tasks.minOf { it.priority }
                tasks.firstOrNull { id.toInt() == it.id }?.let { task ->
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
                is Content.Error -> TasksScreenState.Error(
                    state.tasks,
                    result.exception.message.orEmpty()
                )
                is Content.Loading -> TasksScreenState.Loading(state.tasks)
            }
        }
        sideEffect {
            loadList()
        }
    }
}

private fun TasksScreenState.reduceData(data: ToDoList): TasksScreenState = when (this) {
    is TasksScreenState.Data -> this
    is TasksScreenState.Loading -> TasksScreenState.EMPTY_DATA
    is TasksScreenState.Error -> TasksScreenState.EMPTY_DATA
}.copy(
    tasks = data.tasks.sortedWith(compareByDescending(Task::priority).thenBy(Task::modifiedTime))
)

private fun TasksScreenState.reduceData(
    reducer: TasksScreenState.Data.() -> TasksScreenState.Data
): TasksScreenState =
    when (this) {
        is TasksScreenState.Data -> this.reducer()
        is TasksScreenState.Loading -> TasksScreenState.EMPTY_DATA
        is TasksScreenState.Error -> TasksScreenState.EMPTY_DATA
    }