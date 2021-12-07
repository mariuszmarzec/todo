package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.Store2
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

    override fun removeTask(idToRemove: Int) = sideEffectIntent {
        dialogDelegate.closeDialog()

        intent<Content<Unit>> {
            onTrigger {
                state.ifDataAvailable {
                    if (isRemoveWithCheckBoxChecked(this)) {
                        todoRepository.removeTaskWithSubtasks(taskById(idToRemove))
                    } else {
                        todoRepository.removeTask(idToRemove)
                    }
                }
            }
        }
    }

    private fun isRemoveWithCheckBoxChecked(
        data: DATA
    ): Boolean = (data.dialog as? DialogState.RemoveDialogWithCheckBox)?.checked == true

    override fun onRemoveButtonClick(id: Int) = sideEffectIntent {
        state.ifDataAvailable {
            val taskToRemove = taskById(id)
            when {
                taskToRemove.subTasks.isNotEmpty() -> {
                    dialogDelegate.showRemoveDialogWithCheckBox(id)
                }
                taskToRemove.description.length > 80 ||
                taskToRemove.description.urls().isNotEmpty() -> {
                    dialogDelegate.showRemoveTaskDialog(id)
                }
                else -> {
                    removeTaskDelegate.removeTask(id)
                }
            }
        }
    }
}

interface RemoveTaskDelegate {
    fun removeTask(idToRemove: Int)
    fun onRemoveButtonClick(id: Int)
}