package com.marzec.todo.screen.lists

import com.marzec.mvi.State
import com.marzec.mvi.Store
import com.marzec.mvi.mapData
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.extensions.emptyString
import com.marzec.todo.model.ToDoList
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.flow.first

class ListsScreenStore(
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<ListsScreenState>,
    private val todoRepository: TodoRepository
) : Store2<State<ListsScreenState>>(
    stateCache.get(cacheKey) ?: initialState
) {

    suspend fun initialLoad() = intent<Content<List<ToDoList>>> {
        onTrigger {
            todoRepository.observeLists()
        }

        reducer {
            state.reduceDataWithContent(resultNonNull(), ListsScreenState.INITIAL) { result ->
                copy(todoLists = result.data)
            }
        }
        sideEffect {
            println(result)
        }
    }

    suspend fun addNewList() = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(dialog = ListsScreenDialog.AddNewListDialog(emptyString()))
            }
        }
    }

    suspend fun showRemoveListDialog(id: Int) = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(dialog = ListsScreenDialog.RemoveListDialog(id))
            }
        }
    }

    suspend fun onNewListNameChanged(listName: String) = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(
                    dialog = it.dialog?.asInstanceAndReturn<ListsScreenDialog.AddNewListDialog, ListsScreenDialog.AddNewListDialog> {
                        this.copy(inputField = listName)
                    }
                )
            }
        }
    }

    suspend fun onDialogDismissed() = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(dialog = null)
            }
        }
    }

    suspend fun removeList(id: Int) = intent<Content<Unit>> {
        onTrigger { todoRepository.removeList(id) }

        reducer {
            state.reduceDataWithContent(resultNonNull(), ListsScreenState.INITIAL) {
                copy(dialog = null)
            }
        }
    }

    suspend fun onCreateButtonClicked(newListName: String) = intent<Content<List<ToDoList>>> {
        onTrigger {
            println(todoRepository.createList(newListName))
            todoRepository.observeLists()
        }
        reducer {
            state.reduceDataWithContent(resultNonNull(), ListsScreenState.INITIAL) { result ->
                copy(
                    todoLists = result.data,
                    dialog = null
                )
            }
        }
        sideEffect {
            println(result)
        }
    }

    override suspend fun onNewState(newState: State<ListsScreenState>) {
        stateCache.set(cacheKey, newState)
    }
}