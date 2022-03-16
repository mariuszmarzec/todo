package com.marzec.delegate

import com.marzec.mvi.State
import com.marzec.mvi.reduceData
import com.marzec.extensions.asInstanceAndReturn

interface DialogDelegate {
    fun closeDialog()
    fun showRemoveDialogWithCheckBox(idsToRemove: List<Int>)
    fun showRemoveTaskDialog(idsToRemove: List<Int>)
    fun onRemoveWithSubTasksChange()
    fun showSelectUrlDialog(urls: List<String>)
}

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

interface WithDialog<T> {

    val dialog: DialogState

    fun copyWithDialog(dialog: DialogState): T
}

sealed class DialogState {

    data class RemoveDialog(
        val idsToRemove: List<Int>,
    ) : DialogState()

    data class RemoveDialogWithCheckBox(
        val idsToRemove: List<Int>,
        val checked: Boolean = false
    ) : DialogState()

    data class InputDialog(
        val inputField: String,
    ) : DialogState()

    data class SelectOptionsDialog(
        val items: List<Any>,
    ) : DialogState()

    object NoDialog : DialogState()
}
