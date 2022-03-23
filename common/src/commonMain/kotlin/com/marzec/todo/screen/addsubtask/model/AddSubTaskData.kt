package com.marzec.todo.screen.addsubtask.model

import com.marzec.mvi.State
import com.marzec.delegate.WithSelection
import com.marzec.todo.model.Task

data class AddSubTaskData(
    val tasks: List<Task>,
    override val selected: Set<Int>
) : WithSelection<Int, AddSubTaskData> {

    override fun copyWithSelection(selected: Set<Int>): AddSubTaskData = copy(selected = selected)

    override fun allIds(): Set<Int> = tasks.map { it.id }.toSet()

    companion object {
        val DEFAULT = AddSubTaskData(tasks = emptyList(), selected = emptySet())
        val INITIAL = State.Loading(DEFAULT)
    }
}
