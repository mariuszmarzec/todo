package com.marzec.todo.model

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.UpdateTaskDto
import kotlinx.datetime.LocalDateTime

data class Task(
    val id: Int,
    val description: String,
    val addedTime: LocalDateTime,
    val modifiedTime: LocalDateTime,
    val parentTaskId: Int?,
    val subTasks: List<Task>,
    val isToDo: Boolean,
    val priority: Int
)

fun Task.toDto(): TaskDto = TaskDto(
    id = id,
    description = description,
    addedTime = addedTime.toString(),
    modifiedTime = modifiedTime.toString(),
    parentTaskId = parentTaskId,
    subTasks = subTasks.map { it.toDto() },
    isToDo = isToDo,
    priority = priority
)

fun TaskDto.toDomain(): Task = Task(
    id = this.id,
    description = description,
    addedTime = LocalDateTime.parse(addedTime),
    modifiedTime = LocalDateTime.parse(modifiedTime),
    parentTaskId = parentTaskId,
    subTasks = subTasks.map { it.toDomain() },
    isToDo = isToDo,
    priority = priority
)

data class CreateTask(
    val description: String,
    val parentTaskId: Int?,
    val priority: Int? = null,
    val highestPriorityAsDefault: Boolean? = null
)

fun CreateTaskDto.toDomain() = CreateTask(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
)

fun CreateTask.toDto() = CreateTaskDto(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
    highestPriorityAsDefault = highestPriorityAsDefault
)

data class UpdateTask(
    val description: String,
    val parentTaskId: Int?,
    val priority: Int,
    val isToDo: Boolean
)

fun UpdateTaskDto.toDomain() = UpdateTask(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
    isToDo = isToDo
)

fun UpdateTask.toDto() = UpdateTaskDto(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
    isToDo = isToDo
)
