package com.marzec.todo.network

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.TaskDto

interface DataSource {

    suspend fun removeTask(taskId: Int)

    suspend fun getTasks(): List<TaskDto>

    suspend fun addNewTask(createTaskDto: CreateTaskDto)

    suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean
    )
}
