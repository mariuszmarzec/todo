package com.marzec.todo.delegates.reorder

import com.marzec.delegate.StoreDelegate
import com.marzec.mvi.Intent3
import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.State
import com.marzec.mvi.map
import com.marzec.mvi.mapToState
import com.marzec.todo.screen.tasks.model.TasksScreenState
import kotlinx.coroutines.flow.flowOf

interface ReorderDelegate {

    fun enableReorderMode()

    fun disableReorderMode()

    fun onDragged(draggedIndex: Int, targetIndex: Int)
}

class ReorderDelegateImpl : StoreDelegate<State<TasksScreenState>>(), ReorderDelegate {

    override fun enableReorderMode() {
        run(
            enableReorderModeIntent().mapToData {
                onTrigger {
                    flowOf(state.tasks)
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

    private fun <Result : Any> Intent3<ReorderMode, Result>.mapToData(
        setUp: IntentBuilder<TasksScreenState, Result>.(innerIntent: Intent3<ReorderMode, Result>) -> Unit = { }
    ): Intent3<TasksScreenState, Result> =
        map(
            stateReducer = {
                state.copy(reorderMode = it)
            },
            stateMapper = { it.reorderMode },
            setUp = setUp
        )
}
