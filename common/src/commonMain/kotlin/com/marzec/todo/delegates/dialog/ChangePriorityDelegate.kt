package com.marzec.todo.delegates.dialog

import com.marzec.content.Content
import com.marzec.mvi.State
import com.marzec.mvi.StoreDelegate
import com.marzec.todo.model.UpdateTask
import com.marzec.todo.repository.TodoRepository

interface ChangePriorityDelegate {
    fun changePriority(id: Int, newPriority: Int)
}

class ChangePriorityDelegateImpl<DATA : WithTasks<DATA>>(
    private val todoRepository: TodoRepository
) : StoreDelegate<State<DATA>>(), ChangePriorityDelegate {

    override fun changePriority(id: Int, newPriority: Int) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                todoRepository.updateTask(
                    taskId = id,
                    task = UpdateTask(
                        priority = newPriority
                    )
                )
            }
        }
    }
}
