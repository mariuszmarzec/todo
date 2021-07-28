package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentNoChanges
import com.marzec.mvi.reduceData
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.model.Task
import com.marzec.todo.model.ToDoList
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState
import kotlinx.coroutines.flow.flow

class TasksStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<TasksScreenState>,
    val todoRepository: TodoRepository,
    val listId: Int
) : Store2<State<TasksScreenState>>(
    stateCache.get(cacheKey) ?: initialState
) {

    suspend fun loadList() = intent<Content<ToDoList>> {
        onTrigger {
            todoRepository.observeList(listId)
        }

        reducer {
            state.reduceDataWithContent(resultNonNull(), TasksScreenState.EMPTY_DATA) { result ->
                copy(
                    tasks = result.data.tasks.sortedWith(
                        compareByDescending(Task::priority).thenBy(
                            Task::modifiedTime
                        )
                    )
                )
            }
        }
    }

    suspend fun onListItemClicked(id: String) = sideEffectIntent {
        navigationStore.next(Destination.TaskDetails(listId, id.toInt()))
    }

    suspend fun addNewTask() = sideEffectIntent {
        state.asData {
            navigationStore.next(Destination.AddNewTask(listId, null, null))
        }
    }

    suspend fun showRemoveDialog(id: String) = intent<Unit> {
        reducer {
            state.reduceData {
                copy(
                    removeTaskDialog = DialogState.RemoveDialog(
                        idToRemove = id.toInt()
                    )
                )
            }
        }
    }

    suspend fun hideRemoveDialog() = intent<Unit> {
        reducer {
            state.reduceData {
                copy(removeTaskDialog = DialogState.NoDialog)
            }
        }
    }


    suspend fun removeTask(id: Int) = intent<Content<Unit>> {
        onTrigger {
            todoRepository.removeTask(id)
        }

        reducer {
            state.reduceDataWithContent(resultNonNull(), TasksScreenState.EMPTY_DATA) {
                copy(removeTaskDialog = DialogState.NoDialog)
            }
        }

        sideEffect {
            hideRemoveDialog()
        }
    }

    override suspend fun onNewState(newState: State<TasksScreenState>) {
        stateCache.set(cacheKey, newState)
    }

    suspend fun moveToTop(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asDataAndReturn {
                val maxPriority = tasks.maxOf { it.priority }
                tasks.firstOrNull { id.toInt() == it.id }?.let { task ->
                    todoRepository.updateTask(
                        taskId = id.toInt(),
                        description = task.description,
                        parentTaskId = task.parentTaskId,
                        priority = maxPriority.inc(),
                        isToDo = task.isToDo
                    )
                }
            }
        }
        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }
    }

    suspend fun moveToBottom(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asDataAndReturn {
                val minPriority = tasks.minOf { it.priority }
                tasks.firstOrNull { id.toInt() == it.id }?.let { task ->
                    todoRepository.updateTask(
                        taskId = id.toInt(),
                        description = task.description,
                        parentTaskId = task.parentTaskId,
                        priority = minPriority.dec(),
                        isToDo = task.isToDo
                    )
                }
            }
        }
        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }
    }
}
