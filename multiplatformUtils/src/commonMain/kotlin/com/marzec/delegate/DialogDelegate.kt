package com.marzec.delegate

import com.marzec.mvi.State
import com.marzec.mvi.reduceData
import com.marzec.extensions.asInstanceAndReturn

interface DialogDelegate<ID_TYPE> {
    fun closeDialog()
    fun showRemoveDialogWithCheckBox(idsToRemove: List<ID_TYPE>)
    fun showRemoveTaskDialog(idsToRemove: List<ID_TYPE>, id: String = "")
    fun onRemoveWithSubTasksChange()
    fun showSelectUrlDialog(urls: List<String>)
}

class DialogDelegateImpl<ID_TYPE, DATA : WithDialog<ID_TYPE, DATA>> :
    StoreDelegate<State<DATA>>(),
    DialogDelegate<ID_TYPE> {

    override fun closeDialog() = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(DialogState.NoDialog())
            }
        }
    }

    override fun showRemoveDialogWithCheckBox(idsToRemove: List<ID_TYPE>) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(dialog = DialogState.RemoveDialogWithCheckBox(idsToRemove))
            }
        }
    }

    override fun showRemoveTaskDialog(idsToRemove: List<ID_TYPE>, id: String) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(
                    dialog = DialogState.RemoveDialog(idsToRemove, id)
                )
            }
        }
    }

    override fun onRemoveWithSubTasksChange() = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithDialog(
                    dialog = dialog.asInstanceAndReturn<DialogState.RemoveDialogWithCheckBox<ID_TYPE>> {
                        copy(checked = !this.checked)
                    } ?: DialogState.NoDialog()
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

interface WithDialog<ID_TYPE, T> {

    val dialog: DialogState<ID_TYPE>

    fun copyWithDialog(dialog: DialogState<ID_TYPE>): T
}

sealed class DialogState<ID_TYPE> {

    data class RemoveDialog<ID>(
        val idsToRemove: List<ID>,
        val id: String = ""
    ) : DialogState<ID>()

    data class RemoveDialogWithCheckBox<ID>(
        val idsToRemove: List<ID>,
        val checked: Boolean = false
    ) : DialogState<ID>()

    data class InputDialog<ID>(
        val inputField: String,
    ) : DialogState<ID>()

    data class SelectOptionsDialog<ID>(
        val items: List<Any>,
    ) : DialogState<ID>()

    data class NoDialog<ID>(val unit: Unit = Unit) : DialogState<ID>()
}
