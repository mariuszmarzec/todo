package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.todo.api.toDomain
import com.marzec.todo.cache.Cache

class CompositeDataSource(
    private val localDataSource: LocalDataSource,
    private val apiDataSource: ApiDataSource,
    private val memoryCache: Cache
) : DataSource {

    suspend fun init() {
        updateMemory()
    }

    override suspend fun removeTask(taskId: Int) = update {
        removeTask(taskId)
    }

    override suspend fun getTodoLists(): List<ToDoListDto> {
        val todoLists = apiDataSource.getTodoLists()
        localDataSource.init(todoLists)
        return todoLists
    }

    override suspend fun removeList(id: Int) = update {
        removeList(id)
    }

    override suspend fun createToDoList(title: String) = update {
        createToDoList(title)
    }

    override suspend fun addNewTask(listId: Int, createTaskDto: CreateTaskDto) = update {
        addNewTask(listId, createTaskDto)
    }

    override suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean
    ) = update {
        updateTask(taskId, description, parentTaskId, priority, isToDo)
    }

    private suspend fun update(action: suspend DataSource.() -> Unit) {
        localDataSource.action()
        updateMemory()
        apiDataSource.action()
    }

    private suspend fun updateMemory() {
        memoryCache.put(Api.Todo.TODO_LISTS, localDataSource.getTodoLists().map { it.toDomain() })
    }
}