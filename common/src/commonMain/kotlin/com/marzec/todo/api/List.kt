package com.marzec.todo.api

import com.marzec.dto.NullableFieldDto
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
    val scheduler: SchedulerDto? = null,
    val expirationDate: String? = null,
    val shares: List<TaskShareDto> = emptyList(),
    val ownerId: Int? = null
)

@Serializable
data class CreateTaskDto(
    val description: String,
    val parentTaskId: Int? = null,
    val priority: Int? = null,
    val highestPriorityAsDefault: Boolean? = null,
    val scheduler: SchedulerDto?,
    val expirationDate: String? = null,
    val shares: List<TaskShareDto>? = null
)

@Serializable
data class UpdateTaskDto(
    val description: String? = null,
    val parentTaskId: NullableFieldDto<Int>? = null,
    val priority: Int? = null,
    val isToDo: Boolean? = null,
    val scheduler: NullableFieldDto<SchedulerDto>? = null,
    val expirationDate: NullableFieldDto<String>? = null,
    val shares: List<TaskShareDto>? = null
)

@Serializable
data class TaskShareDto(
    val userId: String,
    val permission: String
)

@Serializable
data class LeaveShareDto(
    val id: Int
)

@Serializable
data class SchedulerDto(
    val hour: Int,
    val minute: Int,
    val creationDate: String? = null,
    val startDate: String,
    val lastDate: String? = null,
    val daysOfWeek: List<Int>,
    val dayOfMonth: Int,
    val repeatCount: Int,
    val repeatInEveryPeriod: Int,
    val type: String,
    val options: Map<String, String>? = null
)

@Serializable
data class MarkAsToDoDto(
    val isToDo: Boolean,
    val taskIds: List<Int>,
)
