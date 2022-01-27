package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.reduceData
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.extensions.toggle

interface WithSelection<DATA> {
    val selected: Set<Int>
    fun copyWithSelection(selected: Set<Int>): DATA
}

interface SelectionDelegate {
    fun onSelectedChange(id: Int)
}

class SelectionDelegateImpl<DATA : WithSelection<DATA>> : StoreDelegate<State<DATA>>(),
    SelectionDelegate {

    override fun onSelectedChange(id: Int) = intent<Unit> {
        reducer {
            state.reduceData {
                copyWithSelection(selected.toggle(id))
            }
        }
    }
}
