package com.marzec.todo.network

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.todo.cache.FileCache
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class LocalDataSource(
    private val cache: FileCache
) : DataSource {

    // TODO Implement local data source
    private val tasks = listOf(
        TaskDto(
            id = 1,
            description = "description",
            addedTime = currentTime(),
            modifiedTime = currentTime(),
            parentTaskId = null,
            subTasks = emptyList(),
            isToDo = true,
            priority = 0
        )
    )

    private val lists = listOf(
        ToDoListDto(
            id = 1,
            title = "list",
            tasks = tasks
        )
    )


    override suspend fun removeTask(taskId: Int) {

    }

    override suspend fun getTodoLists(): List<ToDoListDto> {
        return lists
    }

    override suspend fun removeList(id: Int) {

    }

    override suspend fun createToDoList(title: String) {

    }

    override suspend fun addNewTask(listId: Int, createTaskDto: CreateTaskDto) {

    }

    override suspend fun updateTask(
        taskId: Int,
        description: String,
        parentTaskId: Int?,
        priority: Int,
        isToDo: Boolean
    ) {
    }

    private fun currentTime() =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
}