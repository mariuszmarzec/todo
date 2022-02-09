package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.reduceData
import com.marzec.delegate.StoreDelegate
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

    override fun showRemoveDialogWithCheckBox(idsToRemove: List<Int>) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(dialog = DialogState.RemoveDialogWithCheckBox(idsToRemove))
            }
        }
    }

    override fun showRemoveTaskDialog(idsToRemove: List<Int>) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(
                    dialog = DialogState.RemoveDialog(idsToRemove)
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
    fun showRemoveDialogWithCheckBox(idsToRemove: List<Int>)
    fun showRemoveTaskDialog(idsToRemove: List<Int>)
    fun onRemoveWithSubTasksChange()
    fun showSelectUrlDialog(urls: List<String>)
}
