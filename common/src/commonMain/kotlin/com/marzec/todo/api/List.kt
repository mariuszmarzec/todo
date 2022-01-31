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
    val priority: Int
)

@Serializable
data class CreateTaskDto(
    val description: String,
    val parentTaskId: Int? = null,
    val priority: Int? = null,
    val highestPriorityAsDefault: Boolean? = null
)

@Serializable
data class UpdateTaskDto(
    val description: String,
    val parentTaskId: Int? = null,
    val priority: Int,
    val isToDo: Boolean
)
