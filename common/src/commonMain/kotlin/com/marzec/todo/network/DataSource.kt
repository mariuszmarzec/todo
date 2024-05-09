package com.marzec.todo.network

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.MarkAsToDoDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.UpdateTaskDto

interface DataSource {

    suspend fun removeTask(taskId: Int, removeSubtasks: Boolean = false)

    suspend fun getTasks(): List<TaskDto>

    suspend fun copyTask(taskId: Int)

    suspend fun addNewTask(createTaskDto: CreateTaskDto)

    suspend fun updateTask(
        taskId: Int,
        task: UpdateTaskDto
    )

    suspend fun markAsToDo(request: MarkAsToDoDto)
}
