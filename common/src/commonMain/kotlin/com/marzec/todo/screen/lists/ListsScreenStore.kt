package com.marzec.todo.screen.lists

import com.marzec.mvi.State
import com.marzec.mvi.mapData
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.newMvi.oneShotTrigger
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.extensions.emptyString
import com.marzec.todo.model.ToDoList
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationAction
import com.marzec.todo.navigation.model.NavigationOptions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState
import kotlinx.coroutines.delay

class ListsScreenStore(
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<ListsScreenState>,
    private val todoRepository: TodoRepository,
    private val navigationStore: NavigationStore,
    private val loginRepository: LoginRepository
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
    }

    suspend fun addNewList() = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(dialog = DialogState.InputDialog(emptyString()))
            }
        }
    }

    suspend fun showRemoveListDialog(id: Int) = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(dialog = DialogState.RemoveDialog(id))
            }
        }
    }

    suspend fun onNewListNameChanged(listName: String) = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(
                    dialog = it.dialog.asInstanceAndReturn<DialogState.InputDialog> {
                        this.copy(inputField = listName)
                    } ?: it.dialog
                )
            }
        }
    }

    suspend fun onDialogDismissed() = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(dialog = DialogState.NoDialog)
            }
        }
    }

    suspend fun removeList(id: Int) = intent<Content<Unit>> {
        onTrigger { todoRepository.removeList(id) }

        reducer {
            state.reduceDataWithContent(resultNonNull(), ListsScreenState.INITIAL) {
                copy(dialog = DialogState.NoDialog)
            }
        }
    }

    suspend fun onCreateButtonClicked(newListName: String) = intent<Content<Unit>> {
        onTrigger {
            todoRepository.createList(newListName)
        }
        reducer {
            state.reduceDataWithContent(resultNonNull(), ListsScreenState.INITIAL) {
                copy(
                    dialog = DialogState.NoDialog
                )
            }
        }
    }

    suspend fun logout() = intent<Unit> {
        oneShotTrigger { loginRepository.logout() }

        sideEffect {
            navigationStore.next(
                NavigationAction(
                    destination = Destination.Login,
                    NavigationOptions(
                        Destination.Login,
                        popToInclusive = true
                    )
                )
            )
        }
    }


    override suspend fun onNewState(newState: State<ListsScreenState>) {
        stateCache.set(cacheKey, newState)
    }
}