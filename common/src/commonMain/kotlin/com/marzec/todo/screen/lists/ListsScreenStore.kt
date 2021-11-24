package com.marzec.todo.screen.lists

import com.marzec.mvi.State
import com.marzec.mvi.mapData
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.delegates.dialog.DialogDelegate
import com.marzec.todo.extensions.EMPTY_STRING
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.extensions.delegates
import com.marzec.todo.model.ToDoList
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationAction
import com.marzec.todo.navigation.model.NavigationOptions
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.network.Content
import com.marzec.todo.network.ifDataSuspend
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.LoginRepository
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState

class ListsScreenStore(
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<ListsScreenState>,
    private val todoRepository: TodoRepository,
    private val navigationStore: NavigationStore,
    private val loginRepository: LoginRepository,
    private val dialogDelegate: DialogDelegate
) : Store2<State<ListsScreenState>>(
    stateCache.get(cacheKey) ?: initialState
), DialogDelegate by dialogDelegate {

    init {
        delegates(dialogDelegate)
    }

    fun initialLoad() = intent<Content<List<ToDoList>>> {
        onTrigger {
            todoRepository.observeLists()
        }

        reducer {
            state.reduceDataWithContent(resultNonNull(), ListsScreenState.INITIAL) { result ->
                copy(todoLists = result.data)
            }
        }
    }

    fun addNewList() = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(dialog = DialogState.InputDialog(EMPTY_STRING))
            }
        }
    }

    fun showRemoveListDialog(id: Int) = intent<Unit> {
        reducer {
            state.mapData {
                it.copy(dialog = DialogState.RemoveDialog(id))
            }
        }
    }

    fun onNewListNameChanged(listName: String) = intent<Unit> {
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

    fun removeList(id: Int) = sideEffectIntent {
        closeDialog()

        intent<Content<Unit>> {
            onTrigger { todoRepository.removeList(id) }

            reducer {
                state.reduceContentAsSideAction(resultNonNull())
            }
        }
    }

    fun onCreateButtonClicked(newListName: String) = sideEffectIntent {
        closeDialog()

        intent<Content<Unit>> {
            onTrigger {
                todoRepository.createList(newListName)
            }
            reducer {
                state.reduceContentAsSideAction(resultNonNull())
            }
        }
    }

    fun logout() = intent<Content<Unit>> {
        onTrigger {
            loginRepository.logout()
        }

        sideEffect {
            resultNonNull().ifDataSuspend {
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
    }

    override suspend fun onNewState(newState: State<ListsScreenState>) {
        stateCache.set(cacheKey, newState)
    }
}
