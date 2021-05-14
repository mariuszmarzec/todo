package com.marzec.todo.screen.taskdetails.model

import com.marzec.mvi.Store
import com.marzec.todo.extensions.asInstance
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.extensions.getMessage
import com.marzec.todo.model.Task
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository

class TaskDetailsStore(
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: TaskDetailsState,
    todoRepository: TodoRepository,
    listId: Int,
    taskId: Int
) : Store<TaskDetailsState, TaskDetailsActions>(
    stateCache.get(cacheKey) ?: initialState
) {

    init {
        addIntent<TaskDetailsActions.InitialLoad, Content<Task>> {
            onTrigger {
                todoRepository.getTask(listId, taskId)
            }
            reducer {
                result?.let { result ->
                    when (result) {
                        is Content.Data -> state.reduceData(result.data)
                        is Content.Error -> TaskDetailsState.Error(result.getMessage())
                    }
                } ?: state
            }
        }

        addIntent<TaskDetailsActions.Edit> {
            sideEffect {
                state.asInstance<TaskDetailsState.Data> {
                    navigationStore.next(Destination.AddNewTask(listId, taskId, task.parentTaskId))
                }
            }
        }
    }

    override suspend fun onNewState(newState: TaskDetailsState) {
        stateCache.set(cacheKey, newState)
    }
}

private fun TaskDetailsState.reduceData(data: Task): TaskDetailsState =
    this.asInstanceAndReturn<
            TaskDetailsState.Data,
            TaskDetailsState
            > { TaskDetailsState.Data(data) } ?: TaskDetailsState.Data(data)
