package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.delegate.StoreDelegate
import com.marzec.content.Content
import com.marzec.todo.model.toDto
import com.marzec.todo.repository.TodoRepository

interface ChangePriorityDelegate {
    fun changePriority(id: Int, newPriority: Int)
}

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
                        isToDo = task.isToDo,
                        scheduler = task.scheduler
                    )
                }
            }
        }
        reducer {
            state.reduceContentAsSideAction(resultNonNull())
        }
    }
}
