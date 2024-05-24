package com.marzec.todo.delegates.reorder

import com.marzec.mvi.Intent3
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
                items = currentState.items.toMutableList().apply {
                    val draggedItem = get(draggedIndex)
                    val targetItem = get(targetIndex)

                    remove(draggedItem)

                    val targetIndexAfterRemoval = indexOf(targetItem)

                    println(draggedIndex)
                    println(targetIndex)
                    println(this.map { it.description })

                    add(targetIndexAfterRemoval, draggedItem)
                    println(this.map { it.description })

                }
            )
        }
    }
}
