package com.marzec.todo.delegates.reorder

import com.marzec.delegate.StoreDelegate
import com.marzec.mvi.Intent3
import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.IntentContext
import com.marzec.mvi.State
import com.marzec.mvi.map
import com.marzec.mvi.mapToState
import com.marzec.mvi.reduceData
import com.marzec.todo.screen.tasks.model.TasksScreenState
import kotlinx.coroutines.flow.flowOf

interface ReorderDelegate {

    fun enableReorderMode()

    fun disableReorderMode()

    fun onDragged(draggedIndex: Int, targetIndex: Int)
}

class ReorderDelegateImpl : StoreDelegate<State<TasksScreenState>>(), ReorderDelegate {

    private val stateMapper: (TasksScreenState) -> ReorderMode? = { it.reorderMode }

    override fun enableReorderMode() {
        run(
            enableReorderModeIntent().map(
                stateReducer = {
                    state.copy(reorderMode = it(result, state.reorderMode))
                },
                stateMapper = stateMapper
            ) {
                onTrigger {
                    flowOf(state.tasks)
                }
            }.mapToState()
        )
    }

    override fun disableReorderMode() {
        run(
            disableReorderModeIntent().map(
                stateReducer = {
                    state.copy(reorderMode = it(result, state.reorderMode))
                },
                stateMapper = stateMapper
            ).mapToState()
        )
    }

    override fun onDragged(draggedIndex: Int, targetIndex: Int) {
        run(
            onDraggedIntent(draggedIndex, targetIndex).map(
                stateReducer = {
                    state.copy(reorderMode = it(result, state.reorderMode))
                },
                stateMapper = stateMapper
            ).mapToState()
        )
    }
}

