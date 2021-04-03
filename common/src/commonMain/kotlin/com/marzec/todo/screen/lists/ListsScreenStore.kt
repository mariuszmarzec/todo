package com.marzec.todo.screen.lists

import com.marzec.mvi.Store
import com.marzec.todo.model.ToDoList
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository

class ListsScreenStore(
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: ListsScreenState,
    todoRepository: TodoRepository
) : Store<ListsScreenState, ListScreenActions>(
    stateCache.get(cacheKey) ?: initialState
) {
    init {
        addIntent<ListScreenActions.LoadLists, Content<List<ToDoList>>> {
            onTrigger {
                todoRepository.getLists()
            }

            reducer {
                when (val result = resultNonNull()) {
                    is Content.Data -> state.reduceData(result)
                    is Content.Error -> ListsScreenState.Error(result.exception.message.orEmpty())
                }
            }

            sideEffect {
                println(result)
            }
        }

        addIntent<ListScreenActions.AddNewList> {
            sideEffect { println("+ Clicked") }
        }
    }

    override suspend fun onNewState(newState: ListsScreenState) {
        stateCache.set(cacheKey, newState)
    }
}

private fun ListsScreenState.reduceData(data: Content.Data<List<ToDoList>>): ListsScreenState {
    return when (this) {
        is ListsScreenState.Data -> this
        is ListsScreenState.Error -> ListsScreenState.INITIAL
    }.copy(
        todoLists = data.data
    )
}
