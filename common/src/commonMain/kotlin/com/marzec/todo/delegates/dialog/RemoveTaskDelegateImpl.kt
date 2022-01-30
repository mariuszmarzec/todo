package com.marzec.todo.delegates.dialog

import com.marzec.mvi.IntentBuilder
import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.todo.delegates.StoreDelegate
import com.marzec.todo.extensions.urls
import com.marzec.todo.network.Content
import com.marzec.todo.repository.TodoRepository
import com.marzec.todo.view.DialogState

class RemoveTaskDelegateImpl<DATA : WithTasks<DATA>>(
    private val todoRepository: TodoRepository
) : StoreDelegate<State<DATA>>(), RemoveTaskDelegate {

    private lateinit var removeTaskDelegate: RemoveTaskDelegate
    private lateinit var dialogDelegate: DialogDelegate

    @Suppress("Unchecked_Cast")
    override fun init(store: Store3<State<DATA>>) {
        super.init(store)
        removeTaskDelegate = store as RemoveTaskDelegate
        dialogDelegate = store as DialogDelegate
    }

    override fun removeTask(idsToRemove: List<Int>) = sideEffect {
        dialogDelegate.closeDialog()

        intent {
            removeTaskOnTrigger(todoRepository, idsToRemove)
        }
    }

    override fun onRemoveButtonClick(id: Int) {
        onRemoveButtonClick(listOf(id))
    }

    override fun onRemoveButtonClick(ids: List<Int>) = sideEffect {
        state.ifDataAvailable {
            val tasksToRemove = ids.map { taskById(it) }
            when {
                tasksToRemove.any { it.subTasks.isNotEmpty() } -> {
                    dialogDelegate.showRemoveDialogWithCheckBox(ids)
                }
                tasksToRemove.size > 1 || tasksToRemove.any {
                    it.description.length > 80 ||
                            it.description.urls().isNotEmpty()
                } -> {
                    dialogDelegate.showRemoveTaskDialog(ids)
                }
                else -> {
                    removeTaskDelegate.removeTask(ids)
                }
            }
        }
    }
}

interface RemoveTaskDelegate {
    fun removeTask(idsToRemove: List<Int>)
    fun onRemoveButtonClick(id: Int)
    fun onRemoveButtonClick(ids: List<Int>)
}

fun <DATA : WithTasks<DATA>> IntentBuilder<State<DATA>, Content<Unit>>.removeTaskOnTrigger(
    todoRepository: TodoRepository,
    idsToRemove: List<Int>
) {
    onTrigger {
        state.ifDataAvailable {
            val isRemoveWithCheckBoxChecked = isRemoveWithCheckBoxChecked(this)

            if (idsToRemove.size > 1) {
                if (isRemoveWithCheckBoxChecked) {
                    todoRepository.removeTasksWithSubtasks(
                        idsToRemove.map { taskById(it) }
                    )
                } else {
                    todoRepository.removeTasks(idsToRemove)
                }
            } else {
                if (isRemoveWithCheckBoxChecked) {
                    todoRepository.removeTaskWithSubtasks(taskById(idsToRemove.first()))
                } else {
                    todoRepository.removeTask(idsToRemove.first())
                }
            }
        }
    }
}

private fun <DATA : WithTasks<DATA>> isRemoveWithCheckBoxChecked(
    data: DATA
): Boolean = (data.dialog as? DialogState.RemoveDialogWithCheckBox)?.checked == true