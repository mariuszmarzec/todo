package com.marzec.todo.navigation

import com.marzec.screen.pickitemscreen.PickItemOptions
import com.marzec.todo.model.Scheduler
import kotlinx.datetime.LocalDateTime

sealed class TodoDestination : com.marzec.navigation.Destination {

    data object Login : TodoDestination()
    data object Tasks : TodoDestination()
    data class AddNewTask(
        val taskToEditId: Int?,
        val parentTaskId: Int?
    ) : TodoDestination()

    data class TaskDetails(val taskId: Int) : TodoDestination()
    data class AddSubTask(val taskId: Int) : TodoDestination()
    data class Schedule(
        val scheduler: Scheduler? = null,
        val additionalOptionsAvailable: Boolean = false
    ) : TodoDestination()

    data class DatePicker(val date: LocalDateTime) : TodoDestination()
    data class PickItem<ITEM : Any>(val options: PickItemOptions<ITEM>) : TodoDestination()
}
