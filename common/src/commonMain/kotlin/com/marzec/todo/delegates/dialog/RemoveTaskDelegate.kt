package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.extensions.urls
import com.marzec.todo.network.Content
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState

class RemoveTaskDelegate<DATA : WithTasks<DATA>>(
    private val dialogDelegate: DialogDelegate<DATA>,
    private val todoRepository: TodoRepository
) : StoreDelegate<State<DATA>>() {

    private lateinit var store: Store2<State<DATA>>

    fun init(store: Store2<State<DATA>>) {
        this.store = store
    }

    fun removeTask(idToRemove: Int) = intent<Content<Unit>> {
        onTrigger {
            state.ifDataAvailable {
                if (isRemoveWithCheckBoxChecked(this)) {
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

    private fun isRemoveWithCheckBoxChecked(
        data: DATA
    ): Boolean = (data.dialog as? DialogState.RemoveDialogWithCheckBox)?.checked == true

    fun onRemoveButtonClick(id: String) = sideEffectIntent {
        state.ifDataAvailable {
            val idToRemove = id.toInt()
            val taskToRemove = taskById(idToRemove)
            when {
                taskToRemove.subTasks.isNotEmpty() -> {
                    store.delegate(dialogDelegate.showRemoveDialogWithCheckBox(idToRemove))
                }
                taskToRemove.description.length > 80 ||
                taskToRemove.description.urls().isNotEmpty() -> {
                    store.delegate(dialogDelegate.showRemoveTaskDialog(idToRemove))
                }
                else -> {
                    (store as? StoreWithTaskRemove)?.removeTask(idToRemove)
                        ?: store.delegate(removeTask(idToRemove))
                }
            }
        }
    }
}

interface StoreWithTaskRemove {
    fun removeTask(idToRemove: Int)
}