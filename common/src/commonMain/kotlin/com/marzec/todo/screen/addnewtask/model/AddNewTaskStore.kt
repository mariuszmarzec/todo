package com.marzec.todo.screen.addnewtask.model

import com.marzec.mvi.Store
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.extensions.getMessage
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationAction
import com.marzec.todo.navigation.model.NavigationOptions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.network.Content
import com.marzec.todo.network.ifDataSuspend
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository

class AddNewTaskStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: AddNewTaskState,
    todoRepository: TodoRepository,
) : Store<AddNewTaskState, AddNewTaskActions>(
    stateCache.get(cacheKey) ?: initialState
) {
    init {
        addIntent<AddNewTaskActions.InitialLoad, Content<Task>> {
            onTrigger {
                state.asInstanceAndReturn<AddNewTaskState.Data, Content<Task>?> {
                    data.taskId?.let { todoRepository.getTask(data.listId, it) }
                }
            }
            reducer {
                result?.let { result ->
                    when (result) {
                        is Content.Data -> state.reduceData(result.data)
                        is Content.Error -> AddNewTaskState.Error(result.getMessage())
                        is Content.Loading -> AddNewTaskState.Loading
                    }
                } ?: state
            }
        }
        addIntent<AddNewTaskActions.DescriptionChanged> {
            reducer {
                state.reduceData(action.description)
            }
        }
        addIntent<AddNewTaskActions.Add, Content<Unit>> {
            onTrigger {
                state.asInstanceAndReturn<AddNewTaskState.Data, Content<Unit>> {
                    val taskId = data.taskId
                    if (taskId != null) {
                        todoRepository.updateTask(
                            taskId = taskId,
                            description = data.description,
                            parentTaskId = data.parentTaskId,
                            priority = data.priority,
                            isToDo = data.isToDo
                        )
                    } else {
                        todoRepository.addNewTask(data.listId, data.parentTaskId, data.description)
                    }
                }
            }
            sideEffect {
                result?.ifDataSuspend {
                    state.asInstance<AddNewTaskState.Data> {
                        if (data.parentTaskId != null) {
                            val destination =
                                Destination.TaskDetails(data.listId, data.parentTaskId)
                            navigationStore.next(
                                NavigationAction(
                                    destination = destination,
                                    options = NavigationOptions(destination, true)
                                )
                            )
                        } else {
                            navigationStore.goBack()
                        }
                    }
                }
            }
        }
    }

    override suspend fun onNewState(newState: AddNewTaskState) {
        stateCache.set(cacheKey, newState)
    }
}

private fun AddNewTaskState.reduceData(description: String): AddNewTaskState = when (this) {
    is AddNewTaskState.Data -> this.copy(data = this.data.copy(description = description))
    AddNewTaskState.Loading -> this
    is AddNewTaskState.Error -> this.copy()
}

private fun AddNewTaskState.reduceData(task: Task): AddNewTaskState = when (this) {
    is AddNewTaskState.Data -> this.copy(
        data = this.data.copy(
            parentTaskId = task.parentTaskId,
            description = task.description,
            priority = task.priority,
            isToDo = task.isToDo
        )
    )
    AddNewTaskState.Loading -> this
    is AddNewTaskState.Error -> this.copy()
}
