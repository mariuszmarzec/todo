package com.marzec.todo.screen.addsubtask.model

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentNoChanges
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.network.mapData
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

class AddSubTaskStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<AddSubTaskData>,
    private val todoRepository: TodoRepository,
    private val listId: Int,
    private val taskId: Int
) : Store2<State<AddSubTaskData>>(
    scope, stateCache.get(cacheKey) ?: initialState
) {

    fun initialLoad() = intent<Content<List<Task>>> {
        onTrigger {
            todoRepository.observeList(listId).map { content ->
                content.mapData { toDoList ->
                    toDoList.tasks.filterNot { it.id == taskId }
                }
            }
        }
        reducer {
            state.reduceDataWithContent(resultNonNull()) { tasks ->
                this?.copy(tasks  = tasks) ?: AddSubTaskData.DEFAULT
            }
        }
    }

    fun onAddSubTaskClick() = sideEffectIntent {
        navigationStore.next(
            Destination.AddNewTask(
                listId = listId,
                taskToEditId = null,
                parentTaskId = taskId
            )
        )
    }

    fun pinSubtask(id: Int) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                tasks.firstOrNull { id == it.id }?.let { task ->
                    todoRepository.updateTask(
                        taskId = id,
                        description = task.description,
                        parentTaskId = taskId,
                        priority = task.priority,
                        isToDo = task.isToDo
                    )
                }
            }
        }

        reducer {
            state.reduceContentNoChanges(resultNonNull())
        }

        sideEffect {
            result?.asInstance<Content.Data<*>> {
                navigationStore.goBack()
            }
        }
    }

    override suspend fun onNewState(newState: State<AddSubTaskData>) {
        stateCache.set(cacheKey, newState)
    }
}
