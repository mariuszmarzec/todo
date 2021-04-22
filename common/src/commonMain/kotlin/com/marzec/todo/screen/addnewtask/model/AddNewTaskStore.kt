package com.marzec.todo.screen.addnewtask.model

import com.marzec.mvi.Store
import com.marzec.todo.extensions.asInstanceAndReturnSuspend
import com.marzec.todo.extensions.getMessage
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.goBack
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
                state.asInstanceAndReturnSuspend<AddNewTaskState.Data, Content<Task>?> {
                    data.taskId?.let { todoRepository.getTask(data.listId, it) }
                }
            }
            reducer {
                result?.let { result ->
                    when (result) {
                        is Content.Data -> state.reduceData(result.data.description)
                        is Content.Error -> AddNewTaskState.Error(result.getMessage())
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
                state.asInstanceAndReturnSuspend<AddNewTaskState.Data, Content<Unit>> {
                    val taskId = data.taskId
                    if (taskId != null) {
                        todoRepository.updateTask(taskId, data.description)
                    } else {
                        todoRepository.addNewTask(data.listId, data.description)
                    }
                }
            }
            sideEffect {
                result?.ifDataSuspend { navigationStore.goBack() }
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