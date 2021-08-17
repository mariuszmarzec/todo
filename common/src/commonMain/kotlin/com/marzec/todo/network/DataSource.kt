package com.marzec.todo.network

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.ToDoListDto

interface DataSource {
    suspend fun removeTask(taskId: Int)

    suspend fun getTodoLists(): List<ToDoListDto>

    suspend fun removeList(id: Int)

    suspend fun createToDoList(title: String)

    suspend fun addNewTask(listId: Int, createTaskDto: CreateTaskDto)

    suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean
    )
}