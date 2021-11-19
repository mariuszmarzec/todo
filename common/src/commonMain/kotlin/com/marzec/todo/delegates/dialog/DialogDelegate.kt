package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.reduceContentAsSideAction
import com.marzec.mvi.reduceData
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.model.Task
import com.marzec.todo.network.Content
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState

class DialogDelegate<DATA : WithDialog<DATA>> : StoreDelegate<State<DATA>>() {

    fun closeDialog() = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(DialogState.NoDialog)
            }
        }
    }

    fun showRemoveDialogWithCheckBox(idToRemove: Int) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(dialog = DialogState.RemoveDialogWithCheckBox(idToRemove))
            }
        }
    }

    fun showRemoveTaskDialog(id: Int) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(
                    dialog = DialogState.RemoveDialog(idToRemove = id)
                )
            }
        }
    }

    fun onRemoveWithSubTasksChange() = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(
                    dialog = dialog.asInstanceAndReturn<DialogState.RemoveDialogWithCheckBox> {
                        copy(checked = !this.checked)
                    } ?: DialogState.NoDialog
                )
            }
        }
    }

    fun showSelectUrlDialog(urls: List<String>) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(dialog = DialogState.SelectOptionsDialog(urls))
            }
        }
    }

    fun isRemoveWithCheckBoxChecked(
        data: DATA
    ): Boolean = (data.dialog as? DialogState.RemoveDialogWithCheckBox)?.checked == true
}

interface WithTasks<T> : WithDialog<T> {

    fun taskById(taskId: Int): Task
}


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