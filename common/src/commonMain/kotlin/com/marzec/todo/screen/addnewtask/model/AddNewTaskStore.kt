package com.marzec.todo.screen.addnewtask.model

import com.marzec.mvi.Store
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
    private val listId: Int,
    todoRepository: TodoRepository,
) : Store<AddNewTaskState, AddNewTaskActions>(
    stateCache.get(cacheKey) ?: initialState
) {
    init {
        addIntent<AddNewTaskActions.DescriptionChanged> {
            reducer {
                state.reduceData(action.description)
            }
        }
        addIntent<AddNewTaskActions.Add, Content<Unit>> {
            onTrigger {
                (state as? AddNewTaskState.Data)?.let {
                    todoRepository.addNewTask(listId, it.description)
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
    is AddNewTaskState.Data -> this
    AddNewTaskState.Loading -> AddNewTaskState.DEFAULT
    is AddNewTaskState.Error -> AddNewTaskState.DEFAULT
}.copy(
    description = description
)
