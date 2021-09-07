package com.marzec.todo.screen.addsubtask.model

import com.marzec.mvi.newMvi.Store2
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.extensions.asInstanceAndReturnOther
import com.marzec.todo.extensions.getMessage
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.network.mapData
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AddSubTaskStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: AddSubTaskState,
    private val todoRepository: TodoRepository,
    private val listId: Int,
    private val taskId: Int
) : Store2<AddSubTaskState>(
    stateCache.get(cacheKey) ?: initialState
) {

    suspend fun initialLoad() = intent<Content<List<Task>>> {
        onTrigger {
            todoRepository.observeList(listId).map { content ->
                content.mapData { toDoList ->
                    toDoList.tasks.filterNot { it.id == taskId }
                }
            }
        }
        reducer {
            when (val content = resultNonNull()) {
                is Content.Data -> state.reduceData(content.data)
                is Content.Loading -> AddSubTaskState.Loading(state.tasks)
                is Content.Error -> AddSubTaskState.Error(state.tasks, content.getMessage())
            }
        }

    }

    suspend fun onAddSubTaskClick() = sideEffectIntent {
        navigationStore.next(
            Destination.AddNewTask(
                listId = listId,
                taskToEditId = null,
                parentTaskId = taskId
            )
        )
    }

    suspend fun goToSubtaskDetails(id: String) = sideEffectIntent {
        navigationStore.next(Destination.TaskDetails(listId, id.toInt()))
    }

    suspend fun pinSubtask(id: String) = intent<Content<Unit>> {
        onTrigger {
            state.asInstanceAndReturnOther<AddSubTaskState.Data, Flow<Content<Unit>>?> {
                tasks.firstOrNull { id.toInt() == it.id }?.let { task ->
                    todoRepository.updateTask(
                        taskId = id.toInt(),
                        description = task.description,
                        parentTaskId = taskId,
                        priority = task.priority,
                        isToDo = task.isToDo
                    )
                }
            }
        }

        reducer {
            when (val content = resultNonNull()) {
                is Content.Data -> state.reduceData(state.tasks)
                is Content.Loading -> AddSubTaskState.Loading(state.tasks)
                is Content.Error -> AddSubTaskState.Error(state.tasks, content.getMessage())
            }
        }

        sideEffect {
            result?.asInstance<Content.Data<*>> {
                navigationStore.goBack()
            }
        }
    }

    override suspend fun onNewState(newState: AddSubTaskState) {
        stateCache.set(cacheKey, newState)
    }
}

private fun AddSubTaskState.reduceData(tasks: List<Task>): AddSubTaskState =
    this.asInstanceAndReturnOther<
            AddSubTaskState.Data,
            AddSubTaskState
            > { AddSubTaskState.Data(tasks) } ?: AddSubTaskState.Data(tasks)
