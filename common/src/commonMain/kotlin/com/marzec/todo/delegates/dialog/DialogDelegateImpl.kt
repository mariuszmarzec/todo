package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.newMvi.IntentBuilder
import com.marzec.mvi.reduceData
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.extensions.asInstanceAndReturn
import com.marzec.todo.view.DialogState

class DialogDelegateImpl<DATA : WithDialog<DATA>> :
    StoreDelegate<State<DATA>>(),
    DialogDelegate {

    override fun closeDialog() = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(DialogState.NoDialog)
            }
        }
    }

    override fun showRemoveDialogWithCheckBox(idToRemove: Int) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(dialog = DialogState.RemoveDialogWithCheckBox(idToRemove))
            }
        }
    }

    override fun showRemoveTaskDialog(id: Int) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(
                    dialog = DialogState.RemoveDialog(idToRemove = id)
                )
            }
        }
    }

    override fun onRemoveWithSubTasksChange() = intent<Unit> {
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

    override fun showSelectUrlDialog(urls: List<String>) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(dialog = DialogState.SelectOptionsDialog(urls))
            }
        }
    }
}

interface DialogDelegate {
    fun closeDialog()
    fun showRemoveDialogWithCheckBox(idToRemove: Int)
    fun showRemoveTaskDialog(id: Int)
    fun onRemoveWithSubTasksChange()
    fun showSelectUrlDialog(urls: List<String>)
}
