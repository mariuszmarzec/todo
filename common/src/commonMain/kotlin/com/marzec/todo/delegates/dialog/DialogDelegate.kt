package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.reduceData
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.delegates.BaseDelegate
import com.marzec.todo.view.DialogState

class DialogDelegate<DATA : WithDialog<DATA>>: BaseDelegate<State<DATA>>() {

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

    fun showRemoveTaskDialog(subtaskId: Int) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(
                    dialog = DialogState.RemoveDialog(idToRemove = subtaskId)
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

    fun isRemoveWithCheckBoxChecked(
        data: DATA
    ): Boolean = (data.dialog as? DialogState.RemoveDialogWithCheckBox)?.checked == true
}