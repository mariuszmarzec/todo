package com.marzec.todo.screen.addsubtask.model

import com.marzec.delegate.WithSearch
import com.marzec.mvi.State
import com.marzec.delegate.WithSelection
import com.marzec.todo.model.Task
import com.marzec.view.SearchState

data class AddSubTaskData(
    val tasks: List<Task>,
    override val selected: Set<Int>,
    override val search: SearchState
) : WithSelection<Int, AddSubTaskData>, WithSearch<AddSubTaskData> {

    override fun copyWithSelection(selected: Set<Int>): AddSubTaskData = copy(selected = selected)

    override fun allIds(): Set<Int> = tasks.map { it.id }.toSet()

    override fun copyWithSearch(search: SearchState): AddSubTaskData = copy(search = search)

    companion object {
        val DEFAULT = AddSubTaskData(tasks = emptyList(), selected = emptySet(), search = SearchState(
            value = "",
            focused = false
        ))
        val INITIAL = State.Loading(DEFAULT)
    }
}
