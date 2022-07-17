package com.marzec.todo.network

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.MarkAsToDoDto
import com.marzec.todo.api.SchedulerDto
import com.marzec.todo.api.TaskDto

interface DataSource {

    suspend fun removeTask(taskId: Int)

    suspend fun removeTask(taskId: Int, removeSubtasks: Boolean)

    suspend fun getTasks(): List<TaskDto>

    suspend fun copyTask(taskId: Int)

    suspend fun addNewTask(createTaskDto: CreateTaskDto)

    suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean,
        scheduler: SchedulerDto?
    )

    suspend fun markAsToDo(request: MarkAsToDoDto)
}
