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
                resultNonNull().reduceContent(state)
            }

            sideEffect {
                println(result)
            }
        }

        addIntent<ListScreenActions.AddNewList> {
            reducer {
                (state as? ListsScreenState.Data)?.copy(
                    addNewListDialog = state.addNewListDialog.copy(visible = true)
                ) ?: state
            }
        }

        addIntent<ListScreenActions.NewListNameChanged> {
            reducer {
                (state as? ListsScreenState.Data)?.copy(
                    addNewListDialog = state.addNewListDialog.copy(inputField = action.text)
                ) ?: state
            }
        }

        addIntent<ListScreenActions.DialogDismissed> {
            reducer {
                (state as? ListsScreenState.Data)?.copy(
                    addNewListDialog = state.addNewListDialog.copy(
                        inputField = "",
                        visible = false
                    )
                ) ?: state
            }
        }

        addIntent<ListScreenActions.CreateButtonClicked, Content<List<ToDoList>>> {
            onTrigger {
                println(todoRepository.createList(action.newListName))
                todoRepository.getLists()
            }
            reducer {
                (resultNonNull().reduceContent(state) as? ListsScreenState.Data)?.let {
                    it.copy(
                        addNewListDialog = it.addNewListDialog.copy(
                            inputField = "",
                            visible = false
                        )
                    )
                } ?: state
            }
            sideEffect {
                println(result)
            }
        }
    }

    private fun Content<List<ToDoList>>.reduceContent(state: ListsScreenState) =
        when (this) {
            is Content.Data -> state.reduceData(this)
            is Content.Error -> ListsScreenState.Error(this.exception.message.orEmpty())
            is Content.Loading -> ListsScreenState.INITIAL
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
