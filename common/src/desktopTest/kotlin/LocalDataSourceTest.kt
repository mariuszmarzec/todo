package com.marzec.todo.network

import com.marzec.todo.api.CreateTaskDto
import com.marzec.todo.api.TaskDto
import com.marzec.todo.api.ToDoListDto
import com.marzec.time.currentTimeUtil
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test

internal class LocalDataSourceTest {

    @Before
    fun setUp() {
        CurrentTimeUtil.setOtherTime(16, 5, 2021)
    }

    @Test
    fun removeTask() = runBlockingTest {
        val dataSource = LocalDataSource()
        dataSource.createToDoList("todo list 1")
        dataSource.addNewTask(0, stubCreateTaskDto("task 1"))

        dataSource.removeTask(0)

        assertEquals(listOf(todoListDto), dataSource.getTasks())
    }

    @Test
    fun removeTaskWithSubtasks() = runBlockingTest {
        val dataSource = LocalDataSource()
        dataSource.createToDoList("todo list 1")
        dataSource.addNewTask(0, stubCreateTaskDto("task 1"))
        dataSource.addNewTask(0, stubCreateTaskDto("subtask 1", parentTaskId = 0))

        dataSource.removeTask(0)

        assertEquals(
            listOf(todoListDto.copy(tasks = listOf(stubTaskDto(id = 1, "subtask 1")))),
            dataSource.getTasks()
        )
    }
}

val todoListDto = ToDoListDto(0, "todo list 1", emptyList())

fun stubCreateTaskDto(
    description: String,
    parentTaskId: Int? = null,
    priority: Int? = 0,
    highestPriorityAsDefault: Boolean? = null
) = CreateTaskDto(
    description = description,
    parentTaskId = parentTaskId,
    priority = priority,
    highestPriorityAsDefault = highestPriorityAsDefault
)

fun stubTaskDto(
    id: Int = 1,
    description: String = "",
    addedTime: String = "2021-05-16T00:00:00",
    modifiedTime: String = "2021-05-16T00:00:00",
    parentTaskId: Int? = null,
    subTasks: List<TaskDto> = emptyList(),
    isToDo: Boolean = true,
    priority: Int = 0
) = TaskDto(
    id = id,
    description = description,
    addedTime = addedTime,
    modifiedTime = modifiedTime,
    parentTaskId = parentTaskId,
    subTasks = subTasks,
    isToDo = isToDo,
    priority = priority
)
