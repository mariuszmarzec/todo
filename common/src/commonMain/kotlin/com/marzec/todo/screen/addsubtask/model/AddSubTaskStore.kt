package com.marzec.todo.screen.addsubtask.model

import com.marzec.mvi.Store
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

class AddSubTaskStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: AddSubTaskState,
    todoRepository: TodoRepository,
    listId: Int,
    taskId: Int
) : Store<AddSubTaskState, AddSubTaskActions>(
    stateCache.get(cacheKey) ?: initialState
) {

    init {
        addIntent<AddSubTaskActions.InitialLoad, Content<Task>> {
            reducer {
                state.reduceData(emptyList())
            }
        }

        addIntent<AddSubTaskActions.OnAddSubTaskClick> {
            sideEffect {
                state.asInstance<AddSubTaskState.Data> {
                    navigationStore.next(
                        Destination.AddNewTask(
                            listId = listId,
                            taskToEditId = null,
                            parentTaskId = taskId
                        )
                    )
                }
            }
        }
    }

    override suspend fun onNewState(newState: AddSubTaskState) {
        stateCache.set(cacheKey, newState)
    }
}

private fun AddSubTaskState.reduceData(tasks: List<Task>): AddSubTaskState =
    this.asInstanceAndReturn<
            AddSubTaskState.Data,
            AddSubTaskState
            > { AddSubTaskState.Data(tasks) } ?: AddSubTaskState.Data(tasks)
