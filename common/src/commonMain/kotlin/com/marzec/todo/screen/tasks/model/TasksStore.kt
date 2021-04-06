package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.Store
import com.marzec.todo.model.ToDoList
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository

class TasksStore(
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: TasksScreenState,
    todoRepository: TodoRepository,
    listId: Int
) : Store<TasksScreenState, TasksScreenActions>(
    stateCache.get(cacheKey) ?: initialState
) {
    init {
        addIntent<TasksScreenActions.LoadLists, Content<ToDoList>> {
            onTrigger {
                todoRepository.getList(listId)
            }
            reducer {
                when (val result = resultNonNull()) {
                   is Content.Data -> state.reduceData(result.data)
                   is Content.Error -> TasksScreenState.Error(result.exception.message.orEmpty())
                }
            }
        }
    }

    override suspend fun onNewState(newState: TasksScreenState) {
        stateCache.set(cacheKey, newState)
    }
}

private fun TasksScreenState.reduceData(data: ToDoList): TasksScreenState = when (this) {
    is TasksScreenState.Data -> this
    TasksScreenState.Loading -> TasksScreenState.EMPTY_DATA
    is TasksScreenState.Error -> TasksScreenState.EMPTY_DATA
}.copy(
    tasks = data.tasks
)
