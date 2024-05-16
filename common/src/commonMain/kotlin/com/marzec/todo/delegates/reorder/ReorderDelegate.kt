package com.marzec.todo.delegates.reorder

import com.marzec.delegate.StoreDelegate
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

    private val outStateReducer: TasksScreenState.(ReorderMode) -> TasksScreenState = { copy(reorderMode = it) }
    private val outToInState: (TasksScreenState) -> ReorderMode? = { it.reorderMode }

    override fun enableReorderMode() {
        run(
            enableReorderModeIntent().map(
                stateReducer = {
                    val newReorderModeState = it(result, state.reorderMode)
                    state.outStateReducer(newReorderModeState)
                },
                stateMapper = outToInState
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
                    state.outStateReducer(it(result, state.reorderMode))
                },
                stateMapper = outToInState
            ).mapToState()
        )
    }

    override fun onDragged(draggedIndex: Int, targetIndex: Int) {
        run(
            onDraggedIntent(draggedIndex, targetIndex).map(
                stateReducer = {
                    state.outStateReducer(it(result, state.reorderMode))
                },
                stateMapper = outToInState
            ).mapToState()
        )
    }
}

