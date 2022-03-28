package com.marzec.screen.pickitemscreen

import com.marzec.mvi.State

data class PickItemData<ITEM>(
    val items: List<ITEM>
) {

    companion object {
        fun <ITEM> default() = PickItemData(
            items = emptyList<ITEM>(),
        )
        fun <ITEM> initial() = State.Data(
            default<ITEM>()
        )
    }
}