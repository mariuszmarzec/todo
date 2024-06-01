package com.marzec.todo.delegates.reorder

import com.marzec.extensions.swap
import com.marzec.mvi.intent
import com.marzec.todo.model.Task

sealed class ReorderMode {

    data object Disabled : ReorderMode()

    data class Enabled(val items: List<Task>) : ReorderMode()
}

interface WithReorderMode {

    val reorderMode: ReorderMode
}

fun enableReorderModeIntent() = intent<ReorderMode, List<Task>> {
    reducer {
        ReorderMode.Enabled(items = result.orEmpty())
    }
}

fun disableReorderModeIntent() = intent<ReorderMode, Unit> {
    reducer {
        ReorderMode.Disabled
    }
}

fun onDraggedIntent(draggedIndex: Int, targetIndex: Int) = intent<ReorderMode, Unit> {
    reducer {
        when (val currentState = state) {
            ReorderMode.Disabled -> currentState
            is ReorderMode.Enabled -> currentState.copy(
                items = currentState.items.swap(draggedIndex, targetIndex)
            )
        }
    }
}

fun moveUpIntent(elementIndex: Int) = intent<ReorderMode, Unit> {
    reducer {
        when (val currentState = state) {
            ReorderMode.Disabled -> currentState
            is ReorderMode.Enabled -> currentState.copy(
                items = currentState.items.swap(elementIndex, elementIndex - 1)
            )
        }
    }
}

fun moveDownIntent(elementIndex: Int) = intent<ReorderMode, Unit> {
    reducer {
        when (val currentState = state) {
            ReorderMode.Disabled -> currentState
            is ReorderMode.Enabled -> currentState.copy(
                items = currentState.items.swap(elementIndex, elementIndex + 1)
            )
        }
    }
}

private fun List<Task>.swap(elementIndexToReorder: Int, targetIndex: Int) = toMutableList().apply {
    if (elementIndexToReorder in 0..<size && targetIndex in 0..<size) {
        swap(elementIndexToReorder, targetIndex)
    }
}