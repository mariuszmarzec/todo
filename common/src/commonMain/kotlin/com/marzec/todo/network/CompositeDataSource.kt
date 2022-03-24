package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.TaskDto
import com.marzec.cache.Cache
import com.marzec.todo.api.SchedulerDto
import com.marzec.todo.model.toDomain

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

    override suspend fun getTasks(): List<TaskDto> {
        val todoLists = apiDataSource.getTasks()
        localDataSource.init(todoLists)
        return todoLists
    }

    override suspend fun addNewTask(createTaskDto: CreateTaskDto) = update {
        addNewTask(createTaskDto)
    }

    override suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean,
        scheduler: SchedulerDto?
    ) = update {
        updateTask(taskId, description, parentTaskId, priority, isToDo, scheduler)
    }

    private suspend fun update(action: suspend DataSource.() -> Unit) {
        localDataSource.action()
        updateMemory()
        apiDataSource.action()
    }

    private suspend fun updateMemory() {
        memoryCache.put(Api.Todo.TASKS, localDataSource.getTasks().map { it.toDomain() })
    }
}
