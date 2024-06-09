package com.marzec.todo.delegates.reorder

import com.marzec.delegate.StoreDelegate
import com.marzec.mvi.Intent3
import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.State
import com.marzec.mvi.map
import com.marzec.mvi.mapToState
import com.marzec.todo.model.Task
import kotlinx.coroutines.flow.flowOf

interface ReorderDelegate {

    fun enableReorderMode()

    fun disableReorderMode()

    fun onDragged(draggedIndex: Int, targetIndex: Int)

    fun moveUp(elementIndex: Int)

    fun moveDown(elementIndex: Int)
}

class ReorderDelegateImpl<DATA : WithReorderMode>(
    private val tasksToReorder: DATA.() -> List<Task>,
    private val stateReducer: DATA.(newInState: ReorderMode) -> DATA
) : StoreDelegate<State<DATA>>(), ReorderDelegate {

    override fun enableReorderMode() {
        run(
            enableReorderModeIntent().mapToData {
                onTrigger {
                    flowOf(state.tasksToReorder())
                }
            }.mapToState()
        )
    }

    override fun disableReorderMode() {
        run(
            disableReorderModeIntent().mapToData().mapToState()
        )
    }

    override fun onDragged(draggedIndex: Int, targetIndex: Int) {
        run(
            onDraggedIntent(draggedIndex, targetIndex).mapToData().mapToState()
        )
    }

    override fun moveUp(elementIndex: Int) {
        run(
            moveUpIntent(elementIndex).mapToData().mapToState()
        )
    }

    override fun moveDown(elementIndex: Int) {
        run(moveDownIntent(elementIndex).mapToData().mapToState())
    }

    private fun <Result : Any> Intent3<ReorderMode, Result>.mapToData(
        setUp: IntentBuilder<DATA, Result>.(innerIntent: Intent3<ReorderMode, Result>) -> Unit = { }
    ): Intent3<DATA, Result> =
        map(
            stateReducer = { newInState: ReorderMode -> state.stateReducer(newInState) },
            stateMapper = { it.reorderMode },
            setUp = setUp
        )
}
