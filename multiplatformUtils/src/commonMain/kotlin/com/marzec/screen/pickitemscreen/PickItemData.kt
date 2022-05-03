package com.marzec.screen.pickitemscreen

import com.marzec.delegate.WithSelection
import com.marzec.mvi.State

data class PickItemData<ITEM: Any>(
    val options: PickItemOptions<ITEM>,
    val items: List<ITEM>,
    override val selected: Set<String>
) : WithSelection<String, PickItemData<ITEM>> {

    override fun copyWithSelection(selected: Set<String>): PickItemData<ITEM> =
        copy(selected = selected)

    override fun allIds(): Set<String> = items.map { options.mapItemToId(it) }.toSet()

    companion object {
        fun <ITEM: Any> default(options: PickItemOptions<ITEM>) = PickItemData(
            options = options,
            items = emptyList(),
            selected = emptySet()
        )

        fun <ITEM: Any> initial(
            options: PickItemOptions<ITEM>
        ) = State.Data(
            default(options).copy(selected = options.selected)
        )
    }
}
