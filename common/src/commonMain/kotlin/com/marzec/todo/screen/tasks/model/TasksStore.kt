package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.Store
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.getMessage
import com.marzec.todo.model.ToDoList
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository

class TasksStore(
    private val navigationStore: NavigationStore,
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

        addIntent<TasksScreenActions.ListItemClicked> {
            sideEffect {
                navigationStore.next(Destination.AddNewTask(listId, action.id.toInt()))
            }
        }

        addIntent<TasksScreenActions.AddNewTask> {
            sideEffect {
                state.asInstance<TasksScreenState.Data> {
                    navigationStore.next(Destination.AddNewTask(listId, null))
                }
            }
        }

        addIntent<TasksScreenActions.ShowRemoveDialog> {
            reducer {
                state.reduceData {
                    copy(
                        removeTaskDialog = removeTaskDialog.copy(
                            visible = true,
                            idToRemove = action.id
                        )
                    )
                }
            }
        }

        addIntent<TasksScreenActions.HideRemoveDialog> {
            reducer {
                state.reduceData {
                    copy(
                        removeTaskDialog = removeTaskDialog.copy(
                            visible = false
                        )
                    )
                }
            }
        }

        addIntent<TasksScreenActions.RemoveTask, Content<Unit>> {
            onTrigger { todoRepository.removeTask(action.id) }
            reducer {
                when (val result = resultNonNull()) {
                    is Content.Data -> {
                        state.reduceData {
                            copy(
                                removeTaskDialog = removeTaskDialog.copy(
                                    visible = false
                                )
                            )
                        }
                    }
                    is Content.Error -> TasksScreenState.Error(result.getMessage())
                }
            }

            sideEffect {
                sendAction(TasksScreenActions.HideRemoveDialog)
                sendAction(TasksScreenActions.LoadLists)
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

private fun TasksScreenState.reduceData(
    reducer: TasksScreenState.Data.() -> TasksScreenState.Data
): TasksScreenState =
    when (this) {
        is TasksScreenState.Data -> this.reducer()
        TasksScreenState.Loading -> TasksScreenState.EMPTY_DATA
        is TasksScreenState.Error -> TasksScreenState.EMPTY_DATA
    }