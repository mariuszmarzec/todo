package com.marzec.todo.screen.tasks.model

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceDataWithContent
import com.marzec.todo.delegates.dialog.ChangePriorityDelegate
import com.marzec.todo.delegates.dialog.DialogDelegate
import com.marzec.todo.delegates.dialog.RemoveTaskDelegate
import com.marzec.todo.delegates.dialog.SearchDelegate
import com.marzec.todo.delegates.dialog.UrlDelegate
import com.marzec.todo.extensions.delegates
import com.marzec.todo.model.Task
import com.marzec.todo.model.ToDoList
import com.marzec.todo.navigation.model.Destination
import com.marzec.todo.navigation.model.NavigationStore
import com.marzec.todo.navigation.model.next
import com.marzec.todo.network.Content
import com.marzec.todo.preferences.Preferences
import com.marzec.todo.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope

class TasksStore(
    scope: CoroutineScope,
    private val navigationStore: NavigationStore,
    private val cacheKey: String,
    private val stateCache: Preferences,
    initialState: State<TasksScreenState>,
    val todoRepository: TodoRepository,
    val listId: Int,
    private val urlDelegate: UrlDelegate,
    private val dialogDelegate: DialogDelegate,
    private val removeTaskDelegate: RemoveTaskDelegate,
    private val changePriorityDelegate: ChangePriorityDelegate,
    private val searchDelegate: SearchDelegate
) : Store2<State<TasksScreenState>>(
    scope,
    stateCache.get(cacheKey) ?: initialState
), RemoveTaskDelegate by removeTaskDelegate,
    UrlDelegate by urlDelegate,
    DialogDelegate by dialogDelegate,
    SearchDelegate by searchDelegate {

    override val identifier: String
        get() = cacheKey

    init {
        delegates(
            removeTaskDelegate,
            dialogDelegate,
            changePriorityDelegate,
            urlDelegate,
            searchDelegate
        )
    }

    fun loadList() = intent<Content<ToDoList>> {
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

    fun onListItemClicked(id: String) = sideEffectIntent {
        navigationStore.next(Destination.TaskDetails(listId, id.toInt()))
    }

    fun addNewTask() = sideEffectIntent {
        state.asData {
            navigationStore.next(Destination.AddNewTask(listId, null, null))
        }
    }

    override suspend fun onNewState(newState: State<TasksScreenState>) {
        stateCache.set(cacheKey, newState)
    }

    fun moveToTop(id: String) = sideEffectIntent {
        state.ifDataAvailable {
            changePriorityDelegate.changePriority(
                id = id.toInt(),
                newPriority = tasks.maxOf { it.priority }.inc()
            )
        }
    }

    fun moveToBottom(id: String) = sideEffectIntent {
        state.ifDataAvailable {
            changePriorityDelegate.changePriority(
                id = id.toInt(),
                newPriority = tasks.minOf { it.priority }.dec()
            )
        }
    }
}
