package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.network.Content
import com.marzec.todo.repository.TodoRepository

class ChangePriorityDelegateImpl<DATA : WithTasks<DATA>>(
    private val todoRepository: TodoRepository
) : StoreDelegate<State<DATA>>(),
    ChangePriorityDelegate {

    override fun changePriority(id: Int, newPriority: Int) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                taskById(id).let { task ->
                    todoRepository.updateTask(
                        taskId = id,
                        description = task.description,
                        parentTaskId = task.parentTaskId,
                        priority = newPriority,
                        isToDo = task.isToDo
                    )
                }
            }
        }
        reducer {
            state.reduceContentAsSideAction(resultNonNull())
        }
    }
}