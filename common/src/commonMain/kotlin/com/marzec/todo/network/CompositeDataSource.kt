package com.marzec.todo.network

import com.marzec.todo.Api
import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.TaskDto
import com.marzec.cache.Cache
import com.marzec.todo.api.MarkAsToDoDto
import com.marzec.todo.api.UpdateTaskDto
import com.marzec.todo.model.toDomain

class CompositeDataSource(
    private val localDataSource: LocalDataSource,
    private val apiDataSource: ApiDataSource,
    private val memoryCache: Cache
) : DataSource {

    suspend fun init() {
        updateMemory()
    }

    override suspend fun removeTask(taskId: Int, removeSubtasks: Boolean) = update {
        removeTask(taskId, removeSubtasks)
    }

    override suspend fun getAll(): List<TaskDto> {
        val todoLists = apiDataSource.getAll()
        localDataSource.init(todoLists)
        return todoLists
    }

    override suspend fun getById(id: Int): TaskDto = localDataSource.getById(id)

    override suspend fun copyTask(taskId: Int) = update {
        copyTask(taskId)
    }

    override suspend fun markAsToDo(request: MarkAsToDoDto) = update {
        markAsToDo(request)
    }

    override suspend fun create(createTaskDto: CreateTaskDto): TaskDto = update {
        create(createTaskDto)
    }

    override suspend fun update(
        taskId: Int,
        task: UpdateTaskDto
    ): TaskDto = update {
        this.update(taskId, task)
    }

    override suspend fun remove(id: Int) = update {
        remove(id)
    }

    private suspend fun <T> update(action: suspend DataSource.() -> T): T {
        localDataSource.action()
        updateMemory()
        return apiDataSource.action()
    }

    private suspend fun updateMemory() {
        memoryCache.put(Api.Todo.TASKS, localDataSource.getAll().map { it.toDomain() })
    }
}
