package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.reduceData
import com.marzec.delegate.StoreDelegate
import com.marzec.todo.extensions.toggle

interface WithSelection<DATA> {
    val selected: Set<Int>
    fun copyWithSelection(selected: Set<Int>): DATA
    fun allIds(): Set<Int>
}

interface SelectionDelegate {
    fun onSelectedChange(id: Int)
    fun selectAll(ids: Set<Int>)
    fun deselectAll(ids: Set<Int>)
    fun onAllSelectClicked()
}

class SelectionDelegateImpl<DATA : WithSelection<DATA>> : StoreDelegate<State<DATA>>(),
    SelectionDelegate {

    override fun selectAll(ids: Set<Int>) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSelection(selected + ids.toSet())
            }
        }
    }

    override fun deselectAll(ids: Set<Int>) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSelection(selected - ids.toSet())
            }
        }
    }

    override fun onSelectedChange(id: Int) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSelection(selected.toggle(id))
            }
        }
    }

    override fun onAllSelectClicked() = sideEffect {
        state.ifDataAvailable {
            val ids = allIds()
            if (selected.size == ids.size) {
                deselectAll(ids)
            } else {
                selectAll(ids)
            }
        }
    }
}
