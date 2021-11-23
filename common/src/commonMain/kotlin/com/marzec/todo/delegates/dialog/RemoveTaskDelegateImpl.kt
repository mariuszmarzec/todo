package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.extensions.urls
import com.marzec.todo.network.Content
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState

class RemoveTaskDelegateImpl<DATA : WithTasks<DATA>>(
    private val todoRepository: TodoRepository
) : StoreDelegate<State<DATA>>(), RemoveTaskDelegate {

    private lateinit var removeTaskDelegate: RemoveTaskDelegate
    private lateinit var dialogDelegate: DialogDelegate

    @Suppress("Unchecked_Cast")
    override fun init(store: Store2<State<DATA>>) {
        super.init(store)
        removeTaskDelegate = store as RemoveTaskDelegate
        dialogDelegate = store as DialogDelegate
    }

    override fun removeTask(idToRemove: Int) = intent<Content<Unit>> {
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

    override fun onRemoveButtonClick(id: String) = sideEffectIntent {
        state.ifDataAvailable {
            val idToRemove = id.toInt()
            val taskToRemove = taskById(idToRemove)
            when {
                taskToRemove.subTasks.isNotEmpty() -> {
                    dialogDelegate.showRemoveDialogWithCheckBox(idToRemove)
                }
                taskToRemove.description.length > 80 ||
                taskToRemove.description.urls().isNotEmpty() -> {
                    dialogDelegate.showRemoveTaskDialog(idToRemove)
                }
                else -> {
                    removeTaskDelegate.removeTask(idToRemove)
                }
            }
        }
    }
}

interface RemoveTaskDelegate {
    fun removeTask(idToRemove: Int)
    fun onRemoveButtonClick(id: String)
}