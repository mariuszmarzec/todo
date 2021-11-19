package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.network.Content
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState

class RemoveTaskDelegate<DATA : WithTasks<DATA>>(
    private val dialogDelegate: DialogDelegate<DATA>,
    private val todoRepository: TodoRepository
) : StoreDelegate<State<DATA>>() {
    fun removeTask(idToRemove: Int) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                if (dialogDelegate.isRemoveWithCheckBoxChecked(this)) {
                    todoRepository.removeTaskWithSubtasks(taskById(idToRemove))
                } else {
                    todoRepository.removeTask(idToRemove)
                }
            }
        }

        reducer {
            state.reduceContentAsSideAction(resultNonNull()) {
                copyWithDialog(dialog = DialogState.NoDialog)
            }
        }
    }
}
