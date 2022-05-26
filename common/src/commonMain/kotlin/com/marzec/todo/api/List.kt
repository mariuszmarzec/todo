package com.marzec.todo.api

import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: Int,
    val description: String,
    val addedTime: String,
    val modifiedTime: String,
    val parentTaskId: Int?,
    val subTasks: List<TaskDto>,
    val isToDo: Boolean,
    val priority: Int,
    val scheduler: SchedulerDto? = null
)

@Serializable
data class CreateTaskDto(
    val description: String,
    val parentTaskId: Int? = null,
    val priority: Int? = null,
    val highestPriorityAsDefault: Boolean? = null,
    val scheduler: SchedulerDto?
)

@Serializable
data class UpdateTaskDto(
    val description: String,
    val parentTaskId: Int? = null,
    val priority: Int,
    val isToDo: Boolean,
    val scheduler: SchedulerDto? = null
)

@Serializable
data class SchedulerDto(
    val hour: Int,
    val minute: Int,
    val startDate: String,
    val lastDate: String? = null,
    val daysOfWeek: List<Int>,
    val dayOfMonth: Int,
    val repeatCount: Int,
    val repeatInEveryPeriod: Int,
    val type: String
)

@Serializable
data class MarkAsToDoDto(
    val isToDo: Boolean,
    val taskIds: List<Int>,
)
