package com.marzec.todo.api

import com.marzec.todo.model.ToDoList
import com.marzec.todo.model.toDomain
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
data class ToDoListDto(
    val id: Int,
    val title: String,
    val tasks: List<TaskDto>
)

@Serializable
data class CreateTodoListDto(
    val title: String
)

fun ToDoListDto.toDomain() = ToDoList(
    id = id,
    title = title,
    tasks = tasks.map { it.toDomain(id) }
)

@Serializable
data class CreateTaskDto(
    val description: String,
    val parentTaskId: Int? = null,
    val priority: Int
)

@Serializable
data class UpdateTaskDto(
    val description: String,
    val parentTaskId: Int? = null,
    val priority: Int,
    val isToDo: Boolean
)
